package android.webkit;

/** Test-only fixture colocated with the SDK's package-private constructor. */
public final class TestWebResourceError extends WebResourceError {
    private final int code;
    private final CharSequence description;

    public TestWebResourceError(int code, CharSequence description) {
        this.code = code;
        this.description = description;
    }

    @Override public int getErrorCode() { return code; }
    @Override public CharSequence getDescription() { return description; }
}
