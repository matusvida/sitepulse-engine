"""Allow ``python -m app.worker`` to run the standalone processor."""

from app.worker.processor import main

if __name__ == "__main__":
    main()
