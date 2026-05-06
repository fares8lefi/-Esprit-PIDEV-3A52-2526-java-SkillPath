import io
import json
import os
import sys
import time
import traceback
import warnings
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer

# ── Silence ALL warnings before importing transformers ───────────────────────
warnings.filterwarnings("ignore")
os.environ["TRANSFORMERS_VERBOSITY"] = "error"
os.environ["HF_HUB_DISABLE_PROGRESS_BARS"] = "0"

import numpy as np
import soundfile as sf
import torch
from transformers import AutoModelForSpeechSeq2Seq, AutoProcessor

# ── UTF-8 stdout so Arabic prints correctly on Windows ───────────────────────
if sys.stdout.encoding and sys.stdout.encoding.lower() != "utf-8":
    sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding="utf-8", errors="replace")
if sys.stderr.encoding and sys.stderr.encoding.lower() != "utf-8":
    sys.stderr = io.TextIOWrapper(sys.stderr.buffer, encoding="utf-8", errors="replace")

# ── Config ───────────────────────────────────────────────────────────────────
MODEL_ID  = os.getenv("TUNISPEECH_MODEL_ID", "TuniSpeech-AI/whisper-tunisian-dialect")
HOST      = os.getenv("TUNISPEECH_HOST", "127.0.0.1")
PORT      = int(os.getenv("TUNISPEECH_PORT", "5005"))
TARGET_SR = 16_000   # Whisper always needs 16 kHz mono

# ── Device ───────────────────────────────────────────────────────────────────
def detect_device():
    force_cpu = os.getenv("TUNISPEECH_FORCE_CPU", "").strip().lower() in {"1", "true", "yes"}
    if not force_cpu and torch.cuda.is_available():
        return "cuda", torch.float16
    return "cpu", torch.float32

DEVICE, TORCH_DTYPE = detect_device()

print("\n" + "✨" * 30)
print("🚀 TuniSpeech Server Starting")
print(f"📍 Host: {HOST}:{PORT}")
print(f"🤖 Device: {DEVICE.upper()} ({TORCH_DTYPE})")
if DEVICE == "cuda":
    print(f"🖥️  GPU: {torch.cuda.get_device_name(0)}")
print(f"📦 Model: {MODEL_ID}")
print("✨" * 30 + "\n")
sys.stdout.flush()

# ── Audio helpers ─────────────────────────────────────────────────────────────
def resample_to_16k(audio: np.ndarray, orig_sr: int) -> np.ndarray:
    if orig_sr == TARGET_SR:
        return audio
    target_len = int(len(audio) * TARGET_SR / orig_sr)
    return np.interp(
        np.linspace(0, len(audio) - 1, target_len),
        np.arange(len(audio)),
        audio,
    ).astype(np.float32)


def load_audio(path: str):
    """Load WAV → 16 kHz mono float32, normalised to [-1, 1]."""
    audio, sr = sf.read(path, dtype="float32")
    if audio.ndim > 1:
        audio = audio.mean(axis=1)
    if sr != TARGET_SR:
        print(f"🔄 Resampling {sr} Hz → {TARGET_SR} Hz …")
        audio = resample_to_16k(audio, sr)

    # Diagnose amplitude so we can catch too-quiet recordings
    peak = float(np.abs(audio).max())
    rms  = float(np.sqrt(np.mean(audio ** 2)))
    print(f"🔊 Audio stats  peak={peak:.4f}  rms={rms:.6f}")

    # Normalise if the recording is very quiet (common with some mic drivers)
    if peak > 0 and peak < 0.01:
        print("⚡ Audio is very quiet — normalising to 0.9 peak …")
        audio = audio * (0.9 / peak)

    return audio, TARGET_SR


# ── Model globals ─────────────────────────────────────────────────────────────
MODEL     = None
PROCESSOR = None


def build_model():
    """
    Load model + processor separately so we can call model.generate() directly.
    Using generate() bypasses the pipeline's hidden no-speech / logprob filters
    that silently suppress output and return '-'.
    """
    print("⏳ Loading processor …")
    processor = AutoProcessor.from_pretrained(MODEL_ID)

    print("⏳ Loading model weights …")
    model = AutoModelForSpeechSeq2Seq.from_pretrained(
        MODEL_ID,
        torch_dtype=TORCH_DTYPE,
        low_cpu_mem_usage=True,
        use_safetensors=True,
    )
    model = model.to(DEVICE)
    model.eval()
    print(f"✅ Model on {DEVICE.upper()}")
    return model, processor


# ── Transcription ─────────────────────────────────────────────────────────────
def transcribe(audio: np.ndarray, sr: int) -> str:
    """
    Direct model.generate() call — no pipeline, no hidden filters.
    The forced_decoder_ids (language=ar, task=transcribe) come from the
    processor and match what was used during fine-tuning.
    """
    # Build mel-spectrogram input features
    inputs = PROCESSOR(
        audio,
        sampling_rate=sr,
        return_tensors="pt",
    )
    input_features = inputs.input_features.to(DEVICE, dtype=TORCH_DTYPE)

    # forced_decoder_ids are already set by the processor (language=ar, task=transcribe)
    forced_ids = PROCESSOR.get_decoder_prompt_ids(language="ar", task="transcribe")

    with torch.no_grad():
        generated_ids = MODEL.generate(
            input_features,
            forced_decoder_ids=forced_ids,
            # Remove no-speech / logprob filters that suppress output
            no_speech_threshold=None,    # disable → always attempt transcription
            logprob_threshold=None,      # disable → don't throw away uncertain tokens
            # Generous beam search for best accuracy
            num_beams=5,
            max_new_tokens=256,
        )

    text = PROCESSOR.batch_decode(generated_ids, skip_special_tokens=True)
    return (text[0] if text else "").strip()


# ── HTTP helpers ──────────────────────────────────────────────────────────────
def json_response(handler, payload, status=200):
    raw = json.dumps(payload, ensure_ascii=False).encode("utf-8")
    handler.send_response(status)
    handler.send_header("Content-Type", "application/json; charset=utf-8")
    handler.send_header("Content-Length", str(len(raw)))
    handler.end_headers()
    handler.wfile.write(raw)


# ── Request handler ───────────────────────────────────────────────────────────
class TranscribeHandler(BaseHTTPRequestHandler):

    def do_GET(self):
        if self.path == "/health":
            json_response(self, {"status": "ok", "device": DEVICE, "model": MODEL_ID})
        else:
            json_response(self, {"status": "error", "message": "Not found"}, 404)

    def do_POST(self):
        if self.path != "/":
            json_response(self, {"status": "error", "message": "Not found"}, 404)
            return

        t0 = time.time()
        audio_path = None
        try:
            content_length = int(self.headers.get("Content-Length", "0"))
            if content_length <= 0:
                raise ValueError("Empty request body.")

            body       = json.loads(self.rfile.read(content_length).decode("utf-8"))
            audio_path = body.get("audio_path", "").strip()

            if not audio_path:
                raise ValueError("Missing 'audio_path' field.")
            if not os.path.exists(audio_path):
                raise FileNotFoundError(f"Audio file not found: {audio_path}")

            print(f"\n📥 Processing: {os.path.basename(audio_path)}")

            audio, sr = load_audio(audio_path)
            duration  = len(audio) / sr
            print(f"📊 Duration: {duration:.2f}s @ {sr} Hz")

            if duration < 0.2:
                raise ValueError(f"Audio too short ({duration:.2f}s). Speak for at least 0.5 s.")

            text    = transcribe(audio, sr)
            elapsed = time.time() - t0

            if text:
                print(f"✅ Done in {elapsed:.2f}s  →  {text[:120]}{'…' if len(text) > 120 else ''}")
            else:
                print(f"⚠️  Empty result in {elapsed:.2f}s — audio may be silent/noisy.")

            json_response(self, {"status": "success", "text": text})

        except Exception as exc:
            print(f"❌ Error: {exc}")
            traceback.print_exc()
            json_response(self, {"status": "error", "message": str(exc)})

        finally:
            if audio_path and os.path.exists(audio_path):
                try:
                    os.remove(audio_path)
                except Exception:
                    pass

    def log_message(self, fmt, *args):
        pass   # suppress default HTTP access log


# ── Server entry point ────────────────────────────────────────────────────────
def run_server():
    global MODEL, PROCESSOR
    try:
        MODEL, PROCESSOR = build_model()
        print(f"✅ Server listening on http://{HOST}:{PORT}")
        print("[TUNISPEECH_READY]", flush=True)
    except Exception as exc:
        print(f"🛑 FATAL: {exc}")
        traceback.print_exc()
        sys.exit(1)

    server = ThreadingHTTPServer((HOST, PORT), TranscribeHandler)
    server.serve_forever()


if __name__ == "__main__":
    try:
        run_server()
    except KeyboardInterrupt:
        print("\n👋 Server stopped.")
