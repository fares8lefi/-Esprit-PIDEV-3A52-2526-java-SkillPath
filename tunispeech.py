import io
import json
import os
import sys
import time
import traceback
import warnings
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer

import numpy as np
import soundfile as sf
import torch
from transformers import AutoModelForSpeechSeq2Seq, AutoProcessor, pipeline

warnings.filterwarnings("ignore")

# Keep Arabic output readable in Windows terminals.
if sys.stdout.encoding != "utf-8":
    sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding="utf-8")
if sys.stderr.encoding != "utf-8":
    sys.stderr = io.TextIOWrapper(sys.stderr.buffer, encoding="utf-8")

# Quiet transformer logs.
os.environ["TRANSFORMERS_VERBOSITY"] = "error"
os.environ["HF_HUB_DISABLE_PROGRESS_BARS"] = "1"

MODEL_ID = os.getenv("TUNISPEECH_MODEL_ID", "TuniSpeech-AI/whisper-tunisian-dialect")
HOST = os.getenv("TUNISPEECH_HOST", "127.0.0.1")
PORT = int(os.getenv("TUNISPEECH_PORT", "5001"))
CHUNK_LENGTH_SECONDS = int(os.getenv("TUNISPEECH_CHUNK_SECONDS", "30"))


def detect_device():
    force_cpu = os.getenv("TUNISPEECH_FORCE_CPU", "").strip().lower() in {"1", "true", "yes"}
    if force_cpu:
        return "cpu", torch.float32

    if torch.cuda.is_available():
        torch.backends.cudnn.benchmark = True
        return "cuda", torch.float16

    return "cpu", torch.float32


DEVICE, TORCH_DTYPE = detect_device()

print("\n" + "=" * 60)
print(f"Device: {DEVICE.upper()}")
if DEVICE == "cuda":
    print(f"GPU: {torch.cuda.get_device_name(0)}")
    print(f"VRAM: {torch.cuda.get_device_properties(0).total_memory / 1024**3:.1f} GB")
else:
    print("GPU not available. Running on CPU.")
print("=" * 60)
print(f"Loading model: {MODEL_ID}")


def build_pipeline():
    processor = AutoProcessor.from_pretrained(MODEL_ID)
    model = AutoModelForSpeechSeq2Seq.from_pretrained(
        MODEL_ID,
        torch_dtype=TORCH_DTYPE,
        low_cpu_mem_usage=True,
    )
    model = model.to(DEVICE)

    return pipeline(
        "automatic-speech-recognition",
        model=model,
        tokenizer=processor.tokenizer,
        feature_extractor=processor.feature_extractor,
        torch_dtype=TORCH_DTYPE,
        device=0 if DEVICE == "cuda" else -1,
        chunk_length_s=CHUNK_LENGTH_SECONDS,
    )


try:
    PIPE = build_pipeline()
    print("Model loaded. Server is ready for transcription.\n")
except Exception as exc:
    print(f"FATAL: failed to load TuniSpeech model: {exc}")
    traceback.print_exc()
    sys.exit(1)


def json_response(handler, payload, status_code=200):
    raw = json.dumps(payload, ensure_ascii=False).encode("utf-8")
    handler.send_response(status_code)
    handler.send_header("Content-Type", "application/json; charset=utf-8")
    handler.send_header("Content-Length", str(len(raw)))
    handler.end_headers()
    handler.wfile.write(raw)


class TranscribeHandler(BaseHTTPRequestHandler):
    def do_GET(self):
        if self.path == "/health":
            json_response(self, {"status": "ok", "device": DEVICE, "model_id": MODEL_ID}, 200)
            return
        json_response(self, {"status": "error", "message": "Not found"}, 404)

    def do_POST(self):
        if self.path != "/":
            json_response(self, {"status": "error", "message": "Not found"}, 404)
            return

        started_at = time.time()
        audio_path = None
        try:
            body_length = int(self.headers.get("Content-Length", "0"))
            if body_length <= 0:
                raise ValueError("Missing request body.")

            raw = self.rfile.read(body_length)
            data = json.loads(raw.decode("utf-8"))
            audio_path = data.get("audio_path")
            if not audio_path or not os.path.exists(audio_path):
                raise FileNotFoundError(f"Audio file not found: {audio_path}")

            print(f"[REQUEST] Transcribing: {audio_path}")
            audio_input, sample_rate = sf.read(audio_path)
            if audio_input.ndim > 1:
                audio_input = np.mean(audio_input, axis=1)

            audio_input = audio_input.astype(np.float32)
            duration = len(audio_input) / sample_rate
            print(f"Duration: {duration:.2f}s, sample_rate={sample_rate}, device={DEVICE.upper()}")

            result = PIPE(
                {"array": audio_input, "sampling_rate": sample_rate},
                generate_kwargs={"task": "transcribe", "language": "arabic"},
            )
            transcription = result.get("text", "").strip()

            elapsed = time.time() - started_at
            print(f"[OK] {elapsed:.2f}s -> {transcription[:120]}")
            json_response(self, {"status": "success", "text": transcription}, 200)
        except Exception as exc:
            print(f"[ERROR] {exc}")
            traceback.print_exc()
            json_response(self, {"status": "error", "message": str(exc)}, 200)
        finally:
            if audio_path and os.path.exists(audio_path):
                try:
                    os.remove(audio_path)
                except Exception:
                    pass

    def log_message(self, format_str, *args):
        return


def run_server():
    server = ThreadingHTTPServer((HOST, PORT), TranscribeHandler)
    print(f"TuniSpeech server running on http://{HOST}:{PORT}")
    server.serve_forever()


if __name__ == "__main__":
    run_server()
