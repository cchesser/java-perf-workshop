package cchesser.javaperf.mcp.heap;

import java.time.Duration;

public final class HeapOperationException extends RuntimeException {
    private final HeapErrorCode errorCode;

    public HeapOperationException(HeapErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public HeapOperationException(HeapErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public HeapErrorCode errorCode() {
        return errorCode;
    }

    public static HeapOperationException timeout(String operationName, Duration timeout, Throwable cause) {
        return new HeapOperationException(
                HeapErrorCode.TIMEOUT,
                "Operation '%s' timed out after %d seconds".formatted(operationName, timeout.toSeconds()),
                cause
        );
    }

    public static HeapOperationException interrupted(String operationName, Throwable cause) {
        return new HeapOperationException(
                HeapErrorCode.INTERNAL,
                "Operation '%s' was interrupted".formatted(operationName),
                cause
        );
    }
}
