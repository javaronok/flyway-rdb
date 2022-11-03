package org.flywaydb.database.rdb;

import org.flywaydb.core.api.configuration.Configuration;
import org.flywaydb.core.internal.parser.*;

import java.io.IOException;

public class RdbParser extends Parser {

    private static final String TERM_WITH_SPACES = " TERM ";

    public RdbParser(Configuration configuration, ParsingContext parsingContext) {
        super(configuration, parsingContext, 3);
    }

    @Override
    protected Token handleKeyword(PeekingReader reader, ParserContext context, int pos, int line, int col, String keyword) throws IOException {
        if ("SET".equalsIgnoreCase(keyword)) {
            // Try to detect if this is set SET TERM <new terminator><old terminator>
            String possiblyTerm = reader.peek(TERM_WITH_SPACES.length());
            if (TERM_WITH_SPACES.equalsIgnoreCase(possiblyTerm)) {
                reader.swallow(TERM_WITH_SPACES.length());
                String newDelimiter = reader.readUntilExcluding(context.getDelimiter().getDelimiter());
                reader.swallow(context.getDelimiter().getDelimiter().length());
                return new Token(TokenType.NEW_DELIMITER, pos, line, col, newDelimiter.trim(), newDelimiter, context.getParensDepth());
            }
        }
        return super.handleKeyword(reader, context, pos, line, col, keyword);
    }

    @Override
    protected void resetDelimiter(ParserContext context) {
        // Do not reset delimiter as delimiter changes survive beyond a single statement
    }

    @Override
    protected boolean isAlternativeStringLiteral(String peek) {
        // Support RDB 3+ Q-quoted string
        if (peek.length() < 3) {
            return false;
        }
        char firstChar = peek.charAt(0);
        return (firstChar == 'q' || firstChar == 'Q') && peek.charAt(1) == '\'';
    }

    @Override
    protected Token handleAlternativeStringLiteral(PeekingReader reader, ParserContext context, int pos, int line, int col) throws IOException {
        reader.swallow(2);
        String closeQuote = computeAlternativeCloseQuote((char) reader.read());
        reader.swallowUntilExcluding(closeQuote);
        reader.swallow(closeQuote.length());
        return new Token(TokenType.STRING, pos, line, col, null, null, context.getParensDepth());
    }

    private String computeAlternativeCloseQuote(char specialChar) {
        switch (specialChar) {
            case '[':
                return "]'";
            case '(':
                return ")'";
            case '{':
                return "}'";
            case '<':
                return ">'";
            default:
                return specialChar + "'";
        }
    }
}