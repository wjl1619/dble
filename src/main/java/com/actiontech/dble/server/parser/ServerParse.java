/*
* Copyright (C) 2016-2017 ActionTech.
* based on code by MyCATCopyrightHolder Copyright (c) 2013, OpenCloudDB/MyCAT.
* License: http://www.gnu.org/licenses/gpl.html GPL version 2 or higher.
*/
package com.actiontech.dble.server.parser;

import com.actiontech.dble.config.Versions;
import com.actiontech.dble.route.parser.util.ParseUtil;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author mycat
 */
public final class ServerParse {
    private ServerParse() {
    }

    public static final int OTHER = -1;
    public static final int BEGIN = 1;
    public static final int COMMIT = 2;
    public static final int DELETE = 3;
    public static final int INSERT = 4;
    public static final int REPLACE = 5;
    public static final int ROLLBACK = 6;
    public static final int SELECT = 7;
    public static final int SET = 8;
    public static final int SHOW = 9;
    public static final int START = 10;
    public static final int UPDATE = 11;
    public static final int KILL = 12;
    public static final int SAVEPOINT = 13;
    public static final int USE = 14;
    public static final int EXPLAIN = 15;
    public static final int KILL_QUERY = 16;
    public static final int HELP = 17;
    public static final int MYSQL_CMD_COMMENT = 18;
    public static final int MYSQL_COMMENT = 19;
    public static final int CALL = 20;
    public static final int DESCRIBE = 21;
    public static final int LOCK = 22;
    public static final int UNLOCK = 23;
    public static final int LOAD_DATA_INFILE_SQL = 99;
    public static final int DDL = 100;
    public static final int SCRIPT_PREPARE = 101;
    public static final int EXPLAIN2 = 151;


    public static final int MIGRATE = 203;
    private static final Pattern PATTERN = Pattern.compile("(load)+\\s+(data)+\\s+\\w*\\s*(infile)+", Pattern.CASE_INSENSITIVE);
    private static final Pattern CALL_PATTERN = Pattern.compile("\\w*\\;\\s*\\s*(call)+\\s+\\w*\\s*", Pattern.CASE_INSENSITIVE);

    public static int parse(String stmt) {
        int length = stmt.length();
        //FIX BUG FOR SQL SUCH AS /XXXX/SQL
        int rt = -1;
        for (int i = 0; i < length; ++i) {
            switch (stmt.charAt(i)) {
                case ' ':
                case '\t':
                case '\r':
                case '\n':
                    continue;
                case '/':
                    // such as /*!40101 SET character_set_client = @saved_cs_client
                    // */;
                    if (i == 0 && stmt.charAt(1) == '*' && stmt.charAt(2) == '!' && stmt.charAt(length - 2) == '*' &&
                            stmt.charAt(length - 1) == '/') {
                        return MYSQL_CMD_COMMENT;
                    }
                    //fall through
                case '#':
                    i = ParseUtil.comment(stmt, i);
                    if (i + 1 == length) {
                        return MYSQL_COMMENT;
                    }
                    continue;
                case 'A':
                case 'a':
                    rt = aCheck(stmt, i);
                    break;
                case 'B':
                case 'b':
                    rt = beginCheck(stmt, i);
                    break;
                case 'C':
                case 'c':
                    rt = commitOrCallCheckOrCreate(stmt, i);
                    break;
                case 'D':
                case 'd':
                    rt = deleteOrdCheck(stmt, i);
                    break;
                case 'E':
                case 'e':
                    rt = eCheck(stmt, i);
                    break;
                case 'I':
                case 'i':
                    rt = insertCheck(stmt, i);
                    break;
                case 'M':
                case 'm':
                    rt = migrateCheck(stmt, i);
                    break;
            	case 'P':
            	case 'p':
                    rt = prepareCheck(stmt, i);
                    break;
                case 'R':
                case 'r':
                    rt = rCheck(stmt, i);
                    break;
                case 'S':
                case 's':
                    rt = sCheck(stmt, i);
                    break;
                case 'T':
                case 't':
                    rt = tCheck(stmt, i);
                    break;
                case 'U':
                case 'u':
                    rt = uCheck(stmt, i);
                    break;
                case 'K':
                case 'k':
                    rt = killCheck(stmt, i);
                    break;
                case 'H':
                case 'h':
                    rt = helpCheck(stmt, i);
                    break;
                case 'L':
                case 'l':
                    rt = lCheck(stmt, i);
                    break;
                default:
                    break;
            }
            if (rt != OTHER) {
                return rt;
            }
            continue;
        }
        return OTHER;
    }

    private static int eCheck(String stmt, int offset) {
        int sqlType = OTHER;
        if (stmt.length() > offset + 1) {
            char c1 = stmt.charAt(++offset);
            char c2 = stmt.charAt(++offset);
            if (c1 == 'X' || c1 == 'x') {
                switch (c2) {
                case 'E':
                case 'e':
                    sqlType = executeCheck(stmt, offset);
                    break;
                case 'P':
                case 'p':
                    sqlType = explainCheck(stmt, offset);
                    break;
                default:
                    break;
                }
            }
        }
        return sqlType;  
    }

    static int lCheck(String stmt, int offset) {
        if (stmt.length() > offset + 3) {
            char c1 = stmt.charAt(++offset);
            char c2 = stmt.charAt(++offset);
            char c3 = stmt.charAt(++offset);
            if ((c1 == 'O' || c1 == 'o') && (c2 == 'A' || c2 == 'a') &&
                    (c3 == 'D' || c3 == 'd')) {
                Matcher matcher = PATTERN.matcher(stmt);
                return matcher.find() ? LOAD_DATA_INFILE_SQL : OTHER;
            } else if ((c1 == 'O' || c1 == 'o') && (c2 == 'C' || c2 == 'c') &&
                    (c3 == 'K' || c3 == 'k')) {
                return LOCK;
            }
        }

        return OTHER;
    }

    private static int migrateCheck(String stmt, int offset) {
        if (stmt.length() > offset + 7) {
            char c1 = stmt.charAt(++offset);
            char c2 = stmt.charAt(++offset);
            char c3 = stmt.charAt(++offset);
            char c4 = stmt.charAt(++offset);
            char c5 = stmt.charAt(++offset);
            char c6 = stmt.charAt(++offset);


            if ((c1 == 'i' || c1 == 'I') &&
                    (c2 == 'g' || c2 == 'G') &&
                    (c3 == 'r' || c3 == 'R') &&
                    (c4 == 'a' || c4 == 'A') &&
                    (c5 == 't' || c5 == 'T') &&
                    (c6 == 'e' || c6 == 'E')) {
                return MIGRATE;
            }
        }
        return OTHER;
    }

    private static int prepareCheck(String stmt, int offset) {
        if (stmt.length() > offset + 6) {
            char c1 = stmt.charAt(++offset);
            char c2 = stmt.charAt(++offset);
            char c3 = stmt.charAt(++offset);
            char c4 = stmt.charAt(++offset);
            char c5 = stmt.charAt(++offset);
            char c6 = stmt.charAt(++offset);
            char c7 = stmt.charAt(++offset);
            if ((c1 == 'R' || c1 == 'r') && (c2 == 'E' || c2 == 'e') && (c3 == 'P' || c3 == 'p') && (c4 == 'A' || c4 == 'a') &&
                (c5 == 'R' || c5 == 'r') && (c6 == 'E' || c6 == 'e') &&
                (c7 == ' ' || c7 == '\t' || c7 == '\r' || c7 == '\n')) {
                return SCRIPT_PREPARE;
            }
        }
        return OTHER;
    }

    //truncate
    private static int tCheck(String stmt, int offset) {
        if (stmt.length() > offset + 7) {
            char c1 = stmt.charAt(++offset);
            char c2 = stmt.charAt(++offset);
            char c3 = stmt.charAt(++offset);
            char c4 = stmt.charAt(++offset);
            char c5 = stmt.charAt(++offset);
            char c6 = stmt.charAt(++offset);
            char c7 = stmt.charAt(++offset);
            char c8 = stmt.charAt(++offset);

            if ((c1 == 'R' || c1 == 'r') &&
                    (c2 == 'U' || c2 == 'u') &&
                    (c3 == 'N' || c3 == 'n') &&
                    (c4 == 'C' || c4 == 'c') &&
                    (c5 == 'A' || c5 == 'a') &&
                    (c6 == 'T' || c6 == 't') &&
                    (c7 == 'E' || c7 == 'e') &&
                    (c8 == ' ' || c8 == '\t' || c8 == '\r' || c8 == '\n')) {
                return DDL;
            }
        }
        return OTHER;
    }

    //alter table/view/...
    private static int aCheck(String stmt, int offset) {
        if (stmt.length() > offset + 4) {
            char c1 = stmt.charAt(++offset);
            char c2 = stmt.charAt(++offset);
            char c3 = stmt.charAt(++offset);
            char c4 = stmt.charAt(++offset);
            char c5 = stmt.charAt(++offset);
            if ((c1 == 'L' || c1 == 'l') &&
                    (c2 == 'T' || c2 == 't') &&
                    (c3 == 'E' || c3 == 'e') &&
                    (c4 == 'R' || c4 == 'r') &&
                    (c5 == ' ' || c5 == '\t' || c5 == '\r' || c5 == '\n')) {
                return DDL;
            }
        }
        return OTHER;
    }

    //create table/view/...
    private static int createCheck(String stmt, int offset) {
        if (stmt.length() > offset + 5) {
            char c1 = stmt.charAt(++offset);
            char c2 = stmt.charAt(++offset);
            char c3 = stmt.charAt(++offset);
            char c4 = stmt.charAt(++offset);
            char c5 = stmt.charAt(++offset);
            char c6 = stmt.charAt(++offset);
            if ((c1 == 'R' || c1 == 'r') &&
                    (c2 == 'E' || c2 == 'e') &&
                    (c3 == 'A' || c3 == 'a') &&
                    (c4 == 'T' || c4 == 't') &&
                    (c5 == 'E' || c5 == 'e') &&
                    (c6 == ' ' || c6 == '\t' || c6 == '\r' || c6 == '\n')) {
                return DDL;
            }
        }
        return OTHER;
    }

    //drop
    private static int dropCheck(String stmt, int offset) {
        if (stmt.length() > offset + 3) {
            char c1 = stmt.charAt(++offset);
            char c2 = stmt.charAt(++offset);
            char c3 = stmt.charAt(++offset);
            char c4 = stmt.charAt(++offset);
            if ((c1 == 'R' || c1 == 'r') && (c2 == 'O' || c2 == 'o') && (c3 == 'P' || c3 == 'p') &&
                (c4 == ' ' || c4 == '\t' || c4 == '\r' || c4 == '\n')) {
                // add parse comment
                if (stmt.length() > offset + "PREPARE ".length()) {
                    char c5 = stmt.charAt(++offset);
                    char c6 = stmt.charAt(++offset);
                    char c7 = stmt.charAt(++offset);
                    char c8 = stmt.charAt(++offset);
                    char c9 = stmt.charAt(++offset);
                    char c10 = stmt.charAt(++offset);
                    char c11 = stmt.charAt(++offset);
                    char c12 = stmt.charAt(++offset);
                    if ((c5 == 'P' || c5 == 'p') && (c6 == 'R' || c6 == 'r') && (c7 == 'E' || c7 == 'e') && (c8 == 'P' || c8 == 'p') &&
                        (c9 == 'A' || c9 == 'a') && (c10 == 'R' || c10 == 'r') && (c11 == 'E' || c11 == 'e') &&
                        (c12 == ' ' || c12 == '\t' || c12 == '\r' || c12 == '\n')) {
                        return SCRIPT_PREPARE;
                    }
                }
                return DDL;
            }
        }
        return OTHER;
    }

    // delete or drop
    static int deleteOrdCheck(String stmt, int offset) {
        int sqlType = OTHER;
        switch (stmt.charAt((offset + 1))) {
            case 'E':
            case 'e':
                sqlType = dCheck(stmt, offset);
                break;
            case 'R':
            case 'r':
                sqlType = dropCheck(stmt, offset);
                break;
            default:
                sqlType = OTHER;
        }
        return sqlType;
    }

    // HELP' '
    static int helpCheck(String stmt, int offset) {
        if (stmt.length() > offset + "ELP ".length()) {
            char c1 = stmt.charAt(++offset);
            char c2 = stmt.charAt(++offset);
            char c3 = stmt.charAt(++offset);
            if ((c1 == 'E' || c1 == 'e') && (c2 == 'L' || c2 == 'l') &&
                    (c3 == 'P' || c3 == 'p')) {
                return (offset << 8) | HELP;
            }
        }
        return OTHER;
    }

    //EXECUTE' '
    static int executeCheck(String stmt, int offset) {
        if (stmt.length() > offset + "CUTE ".length()) {
            char c1 = stmt.charAt(++offset);
            char c2 = stmt.charAt(++offset);
            char c3 = stmt.charAt(++offset);
            char c4 = stmt.charAt(++offset);
            char c5 = stmt.charAt(++offset);
            if ((c1 == 'C' || c1 == 'c') && (c2 == 'U' || c2 == 'u') && (c3 == 'T' || c3 == 't') && (c4 == 'E' || c4 == 'e')
                && (c5 == ' ' || c5 == '\t' || c5 == '\r' || c5 == '\n')) {
                return SCRIPT_PREPARE;
            }
        }
        return OTHER;
    }

    // EXPLAIN' '
        static int explainCheck(String stmt, int offset) {
        if (stmt.length() > offset + "LAIN ".length()) {
            char c1 = stmt.charAt(++offset);
            char c2 = stmt.charAt(++offset);
            char c3 = stmt.charAt(++offset);
            char c4 = stmt.charAt(++offset);
            char c5 = stmt.charAt(++offset);
            if ((c1 == 'L' || c1 == 'l') && (c2 == 'A' || c2 == 'a') && (c3 == 'I' || c3 == 'i') && (c4 == 'N' || c4 == 'n')) {
                if (ParseUtil.isSpace(c5)) {
                    return (offset << 8) | EXPLAIN;
                } else if (c5 == '2' && (stmt.length() > offset + 1) && ParseUtil.isSpace(stmt.charAt(++offset))) {
                    return (offset << 8) | EXPLAIN2;
                } else {
                    return OTHER;
                }
            }
        }
        return OTHER;
    }

    // KILL' '
    static int killCheck(String stmt, int offset) {
        if (stmt.length() > offset + "ILL ".length()) {
            char c1 = stmt.charAt(++offset);
            char c2 = stmt.charAt(++offset);
            char c3 = stmt.charAt(++offset);
            char c4 = stmt.charAt(++offset);
            if ((c1 == 'I' || c1 == 'i') && (c2 == 'L' || c2 == 'l') &&
                    (c3 == 'L' || c3 == 'l') &&
                    (c4 == ' ' || c4 == '\t' || c4 == '\r' || c4 == '\n')) {
                while (stmt.length() > ++offset) {
                    switch (stmt.charAt(offset)) {
                        case ' ':
                        case '\t':
                        case '\r':
                        case '\n':
                            continue;
                        case 'Q':
                        case 'q':
                            return killQueryCheck(stmt, offset);
                        default:
                            return (offset << 8) | KILL;
                    }
                }
                return OTHER;
            }
        }
        return OTHER;
    }

    // KILL QUERY' '
    static int killQueryCheck(String stmt, int offset) {
        if (stmt.length() > offset + "UERY ".length()) {
            char c1 = stmt.charAt(++offset);
            char c2 = stmt.charAt(++offset);
            char c3 = stmt.charAt(++offset);
            char c4 = stmt.charAt(++offset);
            char c5 = stmt.charAt(++offset);
            if ((c1 == 'U' || c1 == 'u') && (c2 == 'E' || c2 == 'e') &&
                    (c3 == 'R' || c3 == 'r') && (c4 == 'Y' || c4 == 'y') &&
                    (c5 == ' ' || c5 == '\t' || c5 == '\r' || c5 == '\n')) {
                while (stmt.length() > ++offset) {
                    switch (stmt.charAt(offset)) {
                        case ' ':
                        case '\t':
                        case '\r':
                        case '\n':
                            continue;
                        default:
                            return (offset << 8) | KILL_QUERY;
                    }
                }
                return OTHER;
            }
        }
        return OTHER;
    }

    // BEGIN
    static int beginCheck(String stmt, int offset) {
        if (stmt.length() > offset + 4) {
            char c1 = stmt.charAt(++offset);
            char c2 = stmt.charAt(++offset);
            char c3 = stmt.charAt(++offset);
            char c4 = stmt.charAt(++offset);
            if ((c1 == 'E' || c1 == 'e') &&
                    (c2 == 'G' || c2 == 'g') &&
                    (c3 == 'I' || c3 == 'i') &&
                    (c4 == 'N' || c4 == 'n') &&
                    (stmt.length() == ++offset || ParseUtil.isEOF(stmt, offset))) {
                return BEGIN;
            }
        }
        return OTHER;
    }

    // COMMIT
    static int commitCheck(String stmt, int offset) {
        if (stmt.length() > offset + 5) {
            char c1 = stmt.charAt(++offset);
            char c2 = stmt.charAt(++offset);
            char c3 = stmt.charAt(++offset);
            char c4 = stmt.charAt(++offset);
            char c5 = stmt.charAt(++offset);
            if ((c1 == 'O' || c1 == 'o') &&
                    (c2 == 'M' || c2 == 'm') &&
                    (c3 == 'M' || c3 == 'm') &&
                    (c4 == 'I' || c4 == 'i') &&
                    (c5 == 'T' || c5 == 't') &&
                    (stmt.length() == ++offset || ParseUtil.isEOF(stmt, offset))) {
                return COMMIT;
            }
        }

        return OTHER;
    }

    // CALL
    static int callCheck(String stmt, int offset) {
        if (stmt.length() > offset + 3) {
            char c1 = stmt.charAt(++offset);
            char c2 = stmt.charAt(++offset);
            char c3 = stmt.charAt(++offset);
            if ((c1 == 'A' || c1 == 'a') && (c2 == 'L' || c2 == 'l') &&
                    (c3 == 'L' || c3 == 'l')) {
                return CALL;
            }
        }

        return OTHER;
    }

    static int commitOrCallCheckOrCreate(String stmt, int offset) {
        int sqlType = OTHER;
        switch (stmt.charAt((offset + 1))) {
            case 'O':
            case 'o':
                sqlType = commitCheck(stmt, offset);
                break;
            case 'A':
            case 'a':
                sqlType = callCheck(stmt, offset);
                break;
            case 'R':
            case 'r':
                sqlType = createCheck(stmt, offset);
                break;
            default:
                sqlType = OTHER;
        }
        return sqlType;
    }

    // DESCRIBE or desc or DELETE' ' or DEALLOCATE' '
    static int dCheck(String stmt, int offset) {
        int sqlType = OTHER;
        if (stmt.length() > offset + 1) {
            char c1 = stmt.charAt(++offset);
            char c2 = stmt.charAt(++offset);
            if ((c1 == 'E' || c1 == 'e')) {
                switch (c2) {
                case 'A':
                case 'a':
                    sqlType = dealCheck(stmt, offset);
                    break;
                case 'S':
                case 's':
                    sqlType = descCheck(stmt, offset);
                    break;
                case 'L':
                case 'l':
                    sqlType = deleCheck(stmt, offset);
                    break;
                default:
                    break;
                }
            }
        }
        return sqlType;
    }

    static int dealCheck(String stmt, int offset) {
        if (stmt.length() > offset + "LLOCATE ".length()) {
            char c1 = stmt.charAt(++offset);
            char c2 = stmt.charAt(++offset);
            char c3 = stmt.charAt(++offset);
            char c4 = stmt.charAt(++offset);
            char c5 = stmt.charAt(++offset);
            char c6 = stmt.charAt(++offset);
            char c7 = stmt.charAt(++offset);
            char c8 = stmt.charAt(++offset);
            if ((c1 == 'L' || c1 ==  'l') && (c2 == 'L' || c2 ==  'l') && (c3 == 'O' || c3 ==  'o') && (c4 == 'C' || c4 ==  'c') &&
                (c5 == 'A' || c5 ==  'a') && (c6 == 'T' || c6 ==  't') && (c7 == 'E' || c7 ==  'e') &&
                (c8 == ' ' || c8 == '\t' || c8 == '\r' || c8 == '\n')) {
                return SCRIPT_PREPARE;
            }
        }
        return OTHER;
    }

    static int descCheck(String stmt, int offset) {
        if (stmt.length() > offset + "C ".length()) {
            char c1 = stmt.charAt(++offset);
            char c2 = stmt.charAt(++offset);
            if (c1 == 'C' || c1 == 'c') {
                if (c2 == ' ' || c2 == '\t' || c2 == '\r' || c2 == '\n') {
                    return DESCRIBE;
                } else if (c2 == 'R' || c2 == 'r') {
                    if (stmt.length() > offset + "IBE ".length()) {
                        char c3 = stmt.charAt(++offset);
                        char c4 = stmt.charAt(++offset);
                        char c5 = stmt.charAt(++offset);
                        char c6 = stmt.charAt(++offset);
                        if ((c3 == 'I' || c3 == 'i') && (c4 == 'B' || c4 == 'b') && (c5 == 'E' || c5 == 'e') &&
                            (c6 == ' ' || c6 == '\t' || c6 == '\r' || c6 == '\n')) {
                                return DESCRIBE;
                        }
                    }
                }  
            }
        }
        return OTHER;
    }

    static int deleCheck(String stmt, int offset) {
        if (stmt.length() > offset + "ETE ".length()) {
            char c1 = stmt.charAt(++offset);
            char c2 = stmt.charAt(++offset);
            char c3 = stmt.charAt(++offset);
            char c4 = stmt.charAt(++offset);
            if ((c1 == 'E' || c1 == 'e') && (c2 == 'T' || c2 == 't') && (c3 == 'E' || c3 == 'e') &&
                (c4 == ' ' || c4 == '\t' || c4 == '\r' || c4 == '\n')) {
                return DELETE;
            }
        }
        return OTHER;
    }

    // INSERT' '
    static int insertCheck(String stmt, int offset) {
        if (stmt.length() > offset + 6) {
            char c1 = stmt.charAt(++offset);
            char c2 = stmt.charAt(++offset);
            char c3 = stmt.charAt(++offset);
            char c4 = stmt.charAt(++offset);
            char c5 = stmt.charAt(++offset);
            char c6 = stmt.charAt(++offset);
            if ((c1 == 'N' || c1 == 'n') && (c2 == 'S' || c2 == 's') &&
                    (c3 == 'E' || c3 == 'e') && (c4 == 'R' || c4 == 'r') &&
                    (c5 == 'T' || c5 == 't') &&
                    (c6 == ' ' || c6 == '\t' || c6 == '\r' || c6 == '\n')) {
                return INSERT;
            }
        }
        return OTHER;
    }

    static int rCheck(String stmt, int offset) {
        if (stmt.length() > ++offset) {
            switch (stmt.charAt(offset)) {
                case 'E':
                case 'e':
                    return replaceCheck(stmt, offset);
                case 'O':
                case 'o':
                    return rollbackCheck(stmt, offset);
                default:
                    return OTHER;
            }
        }
        return OTHER;
    }

    // REPLACE' '
    static int replaceCheck(String stmt, int offset) {
        if (stmt.length() > offset + 6) {
            char c1 = stmt.charAt(++offset);
            char c2 = stmt.charAt(++offset);
            char c3 = stmt.charAt(++offset);
            char c4 = stmt.charAt(++offset);
            char c5 = stmt.charAt(++offset);
            char c6 = stmt.charAt(++offset);
            if ((c1 == 'P' || c1 == 'p') && (c2 == 'L' || c2 == 'l') &&
                    (c3 == 'A' || c3 == 'a') && (c4 == 'C' || c4 == 'c') &&
                    (c5 == 'E' || c5 == 'e') &&
                    (c6 == ' ' || c6 == '\t' || c6 == '\r' || c6 == '\n')) {
                return REPLACE;
            }
        }
        return OTHER;
    }

    // ROLLBACK
    static int rollbackCheck(String stmt, int offset) {
        if (stmt.length() > offset + 6) {
            char c1 = stmt.charAt(++offset);
            char c2 = stmt.charAt(++offset);
            char c3 = stmt.charAt(++offset);
            char c4 = stmt.charAt(++offset);
            char c5 = stmt.charAt(++offset);
            char c6 = stmt.charAt(++offset);
            if ((c1 == 'L' || c1 == 'l') &&
                    (c2 == 'L' || c2 == 'l') &&
                    (c3 == 'B' || c3 == 'b') &&
                    (c4 == 'A' || c4 == 'a') &&
                    (c5 == 'C' || c5 == 'c') &&
                    (c6 == 'K' || c6 == 'k') &&
                    (stmt.length() == ++offset || ParseUtil.isEOF(stmt, offset))) {
                return ROLLBACK;
            }
        }
        return OTHER;
    }

    static int sCheck(String stmt, int offset) {
        if (stmt.length() > ++offset) {
            switch (stmt.charAt(offset)) {
                case 'A':
                case 'a':
                    return savepointCheck(stmt, offset);
                case 'E':
                case 'e':
                    return seCheck(stmt, offset);
                case 'H':
                case 'h':
                    return showCheck(stmt, offset);
                case 'T':
                case 't':
                    return startCheck(stmt, offset);
                default:
                    return OTHER;
            }
        }
        return OTHER;
    }

    // SAVEPOINT
    static int savepointCheck(String stmt, int offset) {
        if (stmt.length() > offset + 8) {
            char c1 = stmt.charAt(++offset);
            char c2 = stmt.charAt(++offset);
            char c3 = stmt.charAt(++offset);
            char c4 = stmt.charAt(++offset);
            char c5 = stmt.charAt(++offset);
            char c6 = stmt.charAt(++offset);
            char c7 = stmt.charAt(++offset);
            char c8 = stmt.charAt(++offset);
            if ((c1 == 'V' || c1 == 'v') && (c2 == 'E' || c2 == 'e') &&
                    (c3 == 'P' || c3 == 'p') && (c4 == 'O' || c4 == 'o') &&
                    (c5 == 'I' || c5 == 'i') && (c6 == 'N' || c6 == 'n') &&
                    (c7 == 'T' || c7 == 't') &&
                    (c8 == ' ' || c8 == '\t' || c8 == '\r' || c8 == '\n')) {
                return SAVEPOINT;
            }
        }
        return OTHER;
    }

    static int seCheck(String stmt, int offset) {
        if (stmt.length() > ++offset) {
            switch (stmt.charAt(offset)) {
                case 'L':
                case 'l':
                    return selectCheck(stmt, offset);
                case 'T':
                case 't':
                    if (stmt.length() > ++offset) {
                        //support QUERY like this
                        //  /*!dble: sql=SELECT * FROM test where id=99 */set @pin=1;
                        //  call p_test(@pin,@pout);
                        //  select @pout;
                        if (stmt.startsWith("/*!" + Versions.ANNOTATION_NAME) || stmt.startsWith("/*#" + Versions.ANNOTATION_NAME) || stmt.startsWith("/*" + Versions.ANNOTATION_NAME)) {
                            Matcher matcher = CALL_PATTERN.matcher(stmt);
                            if (matcher.find()) {
                                return CALL;
                            }
                        }

                        char c = stmt.charAt(offset);
                        if (c == ' ' || c == '\r' || c == '\n' || c == '\t' || c == '/' || c == '#') {
                            return (offset << 8) | SET;
                        }
                    }
                    return OTHER;
                default:
                    return OTHER;
            }
        }
        return OTHER;
    }

    // SELECT' '
    static int selectCheck(String stmt, int offset) {
        if (stmt.length() > offset + 4) {
            char c1 = stmt.charAt(++offset);
            char c2 = stmt.charAt(++offset);
            char c3 = stmt.charAt(++offset);
            char c4 = stmt.charAt(++offset);
            if ((c1 == 'E' || c1 == 'e') &&
                    (c2 == 'C' || c2 == 'c') &&
                    (c3 == 'T' || c3 == 't') &&
                    (c4 == ' ' || c4 == '\t' || c4 == '\r' || c4 == '\n' || c4 == '/' || c4 == '#')) {
                return (offset << 8) | SELECT;
            }
        }
        return OTHER;
    }

    // SHOW' '
    static int showCheck(String stmt, int offset) {
        if (stmt.length() > offset + 3) {
            char c1 = stmt.charAt(++offset);
            char c2 = stmt.charAt(++offset);
            char c3 = stmt.charAt(++offset);
            if ((c1 == 'O' || c1 == 'o') && (c2 == 'W' || c2 == 'w') &&
                    (c3 == ' ' || c3 == '\t' || c3 == '\r' || c3 == '\n')) {
                return (offset << 8) | SHOW;
            }
        }
        return OTHER;
    }

    // START' '
    static int startCheck(String stmt, int offset) {
        if (stmt.length() > offset + 4) {
            char c1 = stmt.charAt(++offset);
            char c2 = stmt.charAt(++offset);
            char c3 = stmt.charAt(++offset);
            char c4 = stmt.charAt(++offset);
            if ((c1 == 'A' || c1 == 'a') && (c2 == 'R' || c2 == 'r') &&
                    (c3 == 'T' || c3 == 't') &&
                    (c4 == ' ' || c4 == '\t' || c4 == '\r' || c4 == '\n')) {
                return (offset << 8) | START;
            }
        }
        return OTHER;
    }

    // UPDATE' ' | USE' '
    static int uCheck(String stmt, int offset) {
        if (stmt.length() > ++offset) {
            switch (stmt.charAt(offset)) {
                case 'P':
                case 'p':
                    if (stmt.length() > offset + 5) {
                        char c1 = stmt.charAt(++offset);
                        char c2 = stmt.charAt(++offset);
                        char c3 = stmt.charAt(++offset);
                        char c4 = stmt.charAt(++offset);
                        char c5 = stmt.charAt(++offset);
                        if ((c1 == 'D' || c1 == 'd') &&
                                (c2 == 'A' || c2 == 'a') &&
                                (c3 == 'T' || c3 == 't') &&
                                (c4 == 'E' || c4 == 'e') &&
                                (c5 == ' ' || c5 == '\t' || c5 == '\r' || c5 == '\n')) {
                            return UPDATE;
                        }
                    }
                    break;
                case 'S':
                case 's':
                    if (stmt.length() > offset + 2) {
                        char c1 = stmt.charAt(++offset);
                        char c2 = stmt.charAt(++offset);
                        if ((c1 == 'E' || c1 == 'e') &&
                                (c2 == ' ' || c2 == '\t' || c2 == '\r' || c2 == '\n')) {
                            return (offset << 8) | USE;
                        }
                    }
                    break;
                case 'N':
                case 'n':
                    if (stmt.length() > offset + 5) {
                        char c1 = stmt.charAt(++offset);
                        char c2 = stmt.charAt(++offset);
                        char c3 = stmt.charAt(++offset);
                        char c4 = stmt.charAt(++offset);
                        char c5 = stmt.charAt(++offset);
                        if ((c1 == 'L' || c1 == 'l') &&
                                (c2 == 'O' || c2 == 'o') &&
                                (c3 == 'C' || c3 == 'c') &&
                                (c4 == 'K' || c4 == 'k') &&
                                (c5 == ' ' || c5 == '\t' || c5 == '\r' || c5 == '\n')) {
                            return UNLOCK;
                        }
                    }
                    break;
                default:
                    return OTHER;
            }
        }
        return OTHER;
    }
}
