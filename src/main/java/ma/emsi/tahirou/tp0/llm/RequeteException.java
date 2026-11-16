package ma.emsi.tahirou.tp0.llm;

public class RequeteException extends Exception {
    public RequeteException(String message) { super(message); }
    public RequeteException(String message, Throwable cause) { super(message, cause); }
}