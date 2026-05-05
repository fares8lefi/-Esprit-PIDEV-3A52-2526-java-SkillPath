# Local Tunisian STT (GPU) Setup

This project uses:
- JavaFX app (microphone capture + UI)
- `tunispeech.py` local inference server
- Hugging Face model `TuniSpeech-AI/whisper-tunisian-dialect`

## 1. Create Python environment

```powershell
cd C:\Users\Mega-Pc\Desktop\projetpijava
python -m venv .venv
.\.venv\Scripts\Activate.ps1
python -m pip install --upgrade pip
pip install -r requirements-tunispeech.txt
```

If your GPU is NVIDIA, install a CUDA-enabled PyTorch wheel from the official PyTorch command shown at:
- https://pytorch.org/get-started/locally/

Then reinstall torch using that command.

## 2. Verify GPU availability

```powershell
python -c "import torch; print('cuda=', torch.cuda.is_available()); print('gpu=', torch.cuda.get_device_name(0) if torch.cuda.is_available() else 'none')"
```

Expected for fast mode:
- `cuda= True`
- your GPU name appears

## 3. Optional: force Java to use this Python

Set environment variable (recommended so Java always picks the right interpreter):

```powershell
$env:VOICE_PYTHON_EXE = "C:\Users\Mega-Pc\Desktop\projetpijava\.venv\Scripts\python.exe"
```

Persistent (new terminals):

```powershell
setx VOICE_PYTHON_EXE "C:\Users\Mega-Pc\Desktop\projetpijava\.venv\Scripts\python.exe"
```

## 4. Run Java app

```powershell
mvn clean javafx:run
```

In Add Reclamation screen:
- choose voice language `Tounsi`
- click voice button
- speak and stop recording

On first run, model download can take time. Later runs are much faster.

## 5. Troubleshooting

- If Java says no Python found, set `VOICE_PYTHON_EXE`.
- If startup is slow, keep app open while model downloads fully.
- If GPU is not used, check CUDA driver/toolkit and reinstall CUDA-compatible torch.
- If model access is private, set `HF_TOKEN` in environment before launch.
