package Components;

import Models.GameConstants;
import Models.Move;
import Models.Space;
import Models.Tile;
import Models.TileGenerator;
import Session.Session;

import java.util.ArrayList;

/**
 * @Author Bohdan Yevdokymov, Bill Cook
 *
 * Class to validate word and placement
 */
public class Validator {

    public Validator() {
    }

    /**
     * @return An array containing an int:
     *      1 - Valid play, 0 - invalid, -1 - swear word, 2 - bonus word
     *         as well as the updated Move
     */
    public Object[] isValidPlay(Move move) {
        int startX = move.getStartX();
        int startY = move.getStartY();
        boolean horizontal = move.isHorizontal();
        
        // Ensure move does not extend off board
        if (horizontal && startX + move.getWordString().length() >
                Session.getSession().getBoardAsSpaces().length) {
            return new Object[] {0, move};
        } else if (!horizontal && startY + move.getWordString().length() >
                Session.getSession().getBoardAsSpaces()[0].length) {
            return new Object[] {0, move};
        } else if (startX < 0 || startY < 0)
            return new Object[] {0, move};
        
        // Check that move connects to existing tiles
        if (!connectsToTiles(move)) {
            return new Object[] {0, move};
        }
        
        // Get full word, appending any characters on the ends due to placement
        move.setWord(getFullWord(startX, startY, horizontal, 
                move.getWordString()));
        move.updateStartCoordinate(getTrueStart(move.getStartX(), move.getStartY(),move.isHorizontal()));
        startX = move.getStartX();
        startY = move.getStartY();
        String word = move.getWordString();

        // Check if the user has entered a bad word
        int valid = isProfane(word);

        if (valid == -1) {
            return new Object[] {valid, move};
        }
        // Check if the user entered a bonus word
        valid = isBonus(word);
        if (valid != 2) {
            // If not a bonus, Check if word is in dictionary
            valid = isDictionaryWord(word);
        }

        // Check for valid placement on the board
        if (valid <= 0 || checkPlacement(startX, startY, horizontal, word) == 0)
            return new Object[] {0, move};

        //generating offshoot moves and adding them to the validated move
        move.setOffshootMoves(getOffshootMoves(move));
        return new Object[] {valid, move};
    }

    private int[] getTrueStart(int x0, int y0, boolean horiz){
        Space boardLocal[][] = Session.getSession().getBoardAsSpaces();
        if(horiz){
            int x1 = x0;
            while(x1 > 0 && boardLocal[x1-1][y0].getTile() != null){
                x1--;
            }
            int startCoord[] = {x1, y0};
            return startCoord;
        }
        else{
            int y1 = y0;
            while(y1 > 0 && boardLocal[x0][y1-1].getTile() != null){
                y1--;
            }
            int startCoord[] = {x0, y1};
            return startCoord;
        }

    }

    //generating all offshoot moves (auxiliary words created by the main word played) found in the validation process
    private ArrayList<Move> getOffshootMoves(Move move){
        ArrayList<Move> offshootMoves = new ArrayList();
        int x = move.getStartX();
        int y = move.getStartY();
        int len = move.getWordString().length();
        Space boardLocal[][] = Session.getSession().getBoardAsSpaces();
        //handling if main word is horizontal
        if(move.isHorizontal()) {
            for (int i = 0; i < len; i++) {
                //checking to see if current space is a newly played tile or already existing (if the tile was already in use then the vertical is not a new word
                if (boardLocal[x+i][y].getTile() == null) {
                    int y0 = y;
                    while(0 < y0 && boardLocal[x+i][y0-1].getTile() != null) {
                        y0--;
                    }
                    if(y0 == y) {
                        while (y0 + 1 < boardLocal.length && boardLocal[x + i][y0 + 1].getTile() != null) {
                            y0++;
                        }
                    }
                    //getting full vertical offshoot word
                    if(boardLocal[x+i][y0].getTile() != null) {
                        Tile[] fullOffshootWord = getFullWord(x+i, y, false, String.valueOf(move.getWordString().charAt(i)));
                        if (fullOffshootWord.length > 1)
                            //creating move to add to list of offshoot moves
                            offshootMoves.add(new Move(x+i, y0, false, fullOffshootWord, move.getUser()));
                    }
                }
            }
        }
        //getting offshoot moves for a vertical word
        else{
            for(int i = 0; i < len; i++){
                //checking to see if current space is a newly played tile or already existing
                if(boardLocal[x][y+i].getTile() == null){
                    int x0 = x;
                    while(0 < x0 && boardLocal[x0-1][y+i].getTile() != null){
                        x0--;
                    }
                    if(x0 == x) {
                        while (x0 + 1 < boardLocal.length && boardLocal[x0 + 1][y+i].getTile() != null) {
                            x0++;
                        }
                    }
                    //getting full horizontal offshoot word
                    if(boardLocal[x0][y+i].getTile() != null) {
                        Tile[] fullOffshootWord = getFullWord(x, y+i, true, String.valueOf(move.getWordString().charAt(i)));
                        if (fullOffshootWord.length > 1)
                            //creating move and adding it to list of offshoot moves
                            offshootMoves.add(new Move(x0, y+i, true, fullOffshootWord, move.getUser()));
                    }
                }
            }
        }
        return offshootMoves;
    }

    // Check if move connects to existing tiles
    private boolean connectsToTiles(Move move) {
        // Starts from center of board - valid
        if (Session.getSession().firstMove())
            return true;
        else {
            int remaining = move.getWordString().length();
            boolean hor = move.isHorizontal();
            Space boardLocal[][] = Session.getSession().getBoardAsSpaces();
            int x = move.getStartX();
            int y = move.getStartY();
            while (remaining > 0) {
                if (boardLocal[x][y].getTile() != null
                        || (x > 0 && boardLocal[x-1][y].getTile() != null)
                        || (x < GameConstants.BOARD_WIDTH-1 
                        && boardLocal[x+1][y].getTile() != null)
                        || (y > 0 && boardLocal[x][y-1].getTile() != null)
                        || (y < GameConstants.BOARD_WIDTH-1 
                        && boardLocal[x][y+1].getTile() != null))
                    return true;
                else if (hor && x < GameConstants.BOARD_WIDTH)
                    x++;
                else if (!hor && y < GameConstants.BOARD_WIDTH)
                    y++;
                // Move extends off board
                else
                    return false;
                remaining--;
            }
            // Move does not connect
            return false;
        }
    }

    /// Check if word is a dictionary word
    private static int isDictionaryWord(String word) {
        try {
            if (Dictionaries.getDictionaries().getEnglishWords().contains(word.toUpperCase())) {
                return 1;
            }
        } catch (Exception e) {

            //LogWarning(e.getMessage() + "\n" + e.getStackTrace());
        }

        return 0;
    }

    /// Check if word is a curse word
    private int isProfane(String word) {
        try {
            if (Dictionaries.getDictionaries().getBadWords().contains(word.toUpperCase())) {
                return -1;
            }
        } catch (Exception e) {
            //LogWarning(e.getMessage() + "\n" + e.getStackTrace());
        }

        return 1;
    }

    /// Check if word is a bonus word
    private int isBonus(String word) {
        try {
            if (Dictionaries.getDictionaries().getSpecialWords().contains(word.toUpperCase())) {
                return 2;
            }
        } catch (Exception e) {
            //LogWarning(e.getMessage() + "\n" + e.getStackTrace());
        }

        return 1;
    }

    /*
        Appends any extra characters on the end of the word that may have
        been overlooked when submitting a word for validation
        -Bill Cook
     */
    private Tile[] getFullWord(int startX, int startY, boolean horizontal,
            String word) {
        String leftChars = "";
        String rightChars = "";
        boolean finished = false;
        Space[][] boardLocal = Session.getSession().getBoardAsSpaces();
        TileGenerator tg = TileGenerator.getInstance();

        int x = startX;
        int y = startY;
        if (horizontal) {
            while (x > 0 && boardLocal[x - 1][y].getTile() != null) {
                leftChars = boardLocal[x - 1][y].getTile().getLetter()
                        + leftChars;
                x--;
            }
            x = startX + word.length()-1;
            while (x + 1< boardLocal.length
                    && boardLocal[x + 1][y].getTile() != null) {
                rightChars += boardLocal[x + 1][y].getTile().getLetter();
                x++;
            }
            finished = true;
        } else {
            while (y > 0 && boardLocal[x][y - 1].getTile() != null) {
                leftChars = boardLocal[x][y - 1].getTile().getLetter()
                        + leftChars;
                y--;
            }
            y = startY + word.length()-1;
            while (y + 1 < boardLocal[0].length
                    && boardLocal[x][y + 1].getTile() != null) {
                rightChars += boardLocal[x][y + 1].getTile().getLetter();
                y++;
            }
            finished = true;
        }
        word = leftChars + word + rightChars;
        Tile[] wordTiles = new Tile[word.length()];
        for (int i = 0; i < word.length(); i++)
            wordTiles[i] = tg.getTile(word.charAt(i));
        
        return wordTiles;
    }

    /*
        Recursive method that checks validity of word placement
        -Bill Cook
     */
    private int checkPlacement(int x, int y, boolean horizontal,
            String remaining) {
        if (remaining.length() == 0) {
            return 1;
        } else {
            String leftChars = "";
            String rightChars = "";
            boolean finished = false;
            Space[][] boardLocal = Session.getSession().getBoardAsSpaces();

            int x2 = x;
            int y2 = y;
            if (horizontal) {
                while (y2 > 0 && boardLocal[x][y2 - 1].getTile() != null) {
                    leftChars = boardLocal[x][y2 - 1].getTile().getLetter()
                            + leftChars;
                    y2--;
                }
                y2 = y;
                while (y2 + 1< boardLocal[0].length
                        && boardLocal[x][y2 + 1].getTile() != null) {
                    rightChars += boardLocal[x][y2 + 1].getTile().getLetter();
                    y2++;
                }
                finished = true;
            } else {
                while (x2 > 0 && boardLocal[x2 - 1][y].getTile() != null) {
                    leftChars = boardLocal[x2 - 1][y].getTile().getLetter()
                            + leftChars;
                    x2--;
                }
                x2 = x;
                while (x2 + 1< boardLocal.length
                        && boardLocal[x2 + 1][y].getTile() != null) {
                    rightChars += boardLocal[x2 + 1][y].getTile().getLetter();
                    x2++;
                }
                finished = true;
            }
            String word = leftChars + remaining.charAt(0) + rightChars;
            if (isProfane(word) == -1) {
                return -1;
            } else if (word.length() == 1 || isDictionaryWord(word) == 1) {
                return checkPlacement(horizontal ? x + 1 : x, horizontal
                        ? y : y + 1, horizontal, remaining.length() > 0
                        ? remaining.substring(1) : "");
            } else {
                return 0;
            }
        }

    }

}
