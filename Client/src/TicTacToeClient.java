import java.awt.*;
import java.awt.event.*;
import java.net.*;
import java.io.*;
import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Logger;

public class TicTacToeClient extends JFrame implements Runnable{
    private JTextField idField;
    private JTextArea displayArea;
    private JPanel boardPanel, panel2;
    private Square board[][], currentSquare;
    private Socket connection;
    private DataInputStream input;
    private DataOutputStream output;
    private char myMark;
    private boolean myTurn;
    private final char X_MARK = 'X', O_MARK = 'O';
    JButton button = new JButton("Start Over");
    private final char reset = ' ';


    // Set up user-interface and board
    public TicTacToeClient(String host , int port)
    {

        super( "Tic-Tac-Toe Client" );
        Container container = getContentPane();

        // set up JTextArea to display messages to user
        displayArea = new JTextArea( 4, 30 );
        displayArea.setEditable( true );
        container.add( new JScrollPane( displayArea ), BorderLayout.SOUTH );

        // set up panel for squares in board
        boardPanel = new JPanel();
        boardPanel.setLayout( new GridLayout( 3, 3, 0, 0 ) );
        // create board
        board = new Square[ 3 ][ 3 ];

        // When creating a Square, the location argument to the constructor
        // is a value from 0 to 8 indicating the position of the Square on
        // the board. Values 0, 1, and 2 are the first row, values 3, 4,
        // and 5 are the second row. Values 6, 7, and 8 are the third row.
        for ( int row = 0; row < board.length; row++ ) {

            for ( int column = 0; column < board[ row ].length; column++ ) {
                // create Square
                // add 9 new panels to the panel
                board[ row ][ column ] = new Square( ' ', row * 3 + column );
                boardPanel.add( board[ row ][ column ] );
            }
        }

        // textfield to display player's mark
        idField = new JTextField();
        idField.setEditable( false );
        container.add( idField, BorderLayout.NORTH );

        // set up panel to contain boardPanel
        panel2 = new JPanel();
        panel2.add( boardPanel, BorderLayout.CENTER );
        container.add( panel2, BorderLayout.CENTER );
        setSize(300,300);
        setVisible(true);

        panel2.add(button);
        button.setVisible(false);
        button.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                if(e.getSource()== button) {
                    int resetGame = 101;
                    try {
                        output.writeInt( resetGame );
                    }
                    // process problems communicating with server
                    catch ( IOException ioException ) {
                        ioException.printStackTrace();
                    }
                    displayMessage("You Pressed Button First\n");
                }
            }
        });
    } // end method init

    private static final Logger LOGGER = Logger.getLogger("main");

    // Make connection to server and get associated streams.
    // Start separate thread to allow this applet to
    // continually update its output in textarea display.
    public void execute()
    {
        // connect to server, get streams and start outputThread
        try {

            // make connection
            // create a socket
            connection = new Socket( "localhost", 12345 );

            // get streams
            input = new DataInputStream( connection.getInputStream() );
            output = new DataOutputStream( connection.getOutputStream() );
        }

        // catch problems setting up connection and streams
        catch ( IOException ioException ) {
            ioException.printStackTrace();
        }

        // create and start output thread
        //
        Thread outputThread = new Thread( this );
        outputThread.start();

    } // end method start

    // control thread that allows continuous update of displayArea
    public void run()
    {
        // get player's mark (X or O)
        try {
            myMark = input.readChar();
            
            SwingUtilities.invokeLater(
                    new Runnable() {
                        public void run()
                        {
                            idField.setText( "You are player \"" + myMark + "\"" );
                        }
                    }
            );
            myTurn = ( myMark == X_MARK ? true : false );
            LOGGER.info("my Turn = \n"     + myTurn);

            // receive messages sent to client and output them
            while ( true ) {
                processMessage( input.readUTF() );
            }

        }

        // process problems communicating with server
        catch ( IOException ioException ) {
            ioException.printStackTrace();
        }

    }  // end method run

    public static void main( String args[] )
    {
        String host = "localhost";
        int port = 12345;
        if (args.length > 0) {
            host = args[0];
        }
        if (args.length > 1) {
            port = Integer.parseInt(args[1]);
        }

        TicTacToeClient application = new TicTacToeClient(host, port);
        application.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                application.execute();
            }
        });

    }
    // process messages received by client
    private void processMessage( String message )
    {
        // valid move occurred
        if ( message.equals( "Valid move." ) ) {
            displayMessage( "Valid move, please wait.\n" );
            setMark( currentSquare, myMark );
        }

        // invalid move occurred
        else if ( message.equals( "Invalid move, try again" ) ) {
            displayMessage( message + "\n" );
            myTurn = true;
        }

        // opponent moved
        else if ( message.equals( "Opponent moved" ) ) {

            // get move location and update board
            try {
                int location = input.readInt();
                int row = location / 3;
                int column = location % 3;

                setMark(  board[ row ][ column ],
                        ( myMark == X_MARK ? O_MARK : X_MARK ) );
                displayMessage( "Opponent moved. Your turn.\n" );
                myTurn = true;

            } // end try
            // process problems communicating with server
            catch ( IOException ioException ) {
                ioException.printStackTrace();
            }
        }

        else if ( message.equals( "Game Finished \n" ) ) {
            displayMessage( "You Pressed Button First\n" );
            myTurn = true;
        }
        else if(message.equals("Player X Wins \n")){
            displayMessage("Player X Wins \n");
            LOGGER.info("Player X Wins \n");
            button.setVisible(true);
            myTurn = false;
        }
        else if(message.equals("Player O Wins \n")){
            displayMessage("Player O Wins \n");
            LOGGER.info("Player O Wins \n");
            button.setVisible(true);
            myTurn = false;
        }
        else if(message.equals("Draw \n")){
            displayMessage("Game Ended In A Draw\n");
            LOGGER.info("Game Ended In A Draw \n");
            myTurn = false;
            button.setVisible(true);
        }
        else if(message.equals("Board Reset \n")){
            displayMessage("Board Reset \n");
            LOGGER.info("Board Reset \n");
            resetGame();
            button.setVisible(false);
            myTurn = false;
        }
        // simply display message
        else
            displayMessage( message + "\n" );

    } // end method processMessage

    // utility method called from other threads to manipulate
    // outputArea in the event-dispatch thread
    private void displayMessage( final String messageToDisplay )
    {
        // display message from event-dispatch thread of execution
        SwingUtilities.invokeLater(
                new Runnable() {  // inner class to ensure GUI updates properly

                    public void run() // updates displayArea
                    {
                        displayArea.append( messageToDisplay );
                        displayArea.setCaretPosition(displayArea.getText().length() );
                    }

                }  // end inner class

        ); // end call to SwingUtilities.invokeLater
    }

    // utility method to set mark on board in event-dispatch thread
    private void setMark( final Square squareToMark, final char mark )
    {
        SwingUtilities.invokeLater(
                new Runnable() {
                    public void run()
                    {
                        squareToMark.setMark( mark );
                    }
                }
        );
    }

    // send message to server indicating clicked square
    public void sendClickedSquare( int location )
    {
        if ( myTurn ) {
            // send location to server
            try {
                output.writeInt( location );
                myTurn = false;
            }

            // process problems communicating with server
            catch ( IOException ioException ) {
                ioException.printStackTrace();
            }
        }
    }

    public void resetGame(){
        //clear board contents on the gui
        for ( int row = 0; row < board.length; row++ ) {
            for ( int column = 0; column < board[ row ].length; column++ ) {
                setMark(  board[ row][ column ], reset) ;
            }
        }
    }

    // set current Square
    public void setCurrentSquare( Square square )
    {
        currentSquare = square;
    }

    // private inner class for the squares on the board
    private class Square extends JPanel {
        private char mark;
        private int location;

        public Square( char squareMark, int squareLocation )
        {
            mark = squareMark;
            location = squareLocation;

            addMouseListener(
                    new MouseAdapter() {
                        public void mouseReleased( MouseEvent e )
                        {
                            setCurrentSquare( Square.this );
                            sendClickedSquare( getSquareLocation() );
                        }
                    }
            );

        } // end Square constructor

        // return preferred size of Square
        public Dimension getPreferredSize()
        {
            return new Dimension( 30, 30 );
        }

        // return minimum size of Square
        public Dimension getMinimumSize()
        {
            return getPreferredSize();
        }

        // set mark for Square
        public void setMark( char newMark )
        {
            mark = newMark;
            repaint();
        }

        // return Square location
        public int getSquareLocation()
        {
            return location;
        }

        // draw Square
        public void paintComponent( Graphics g )
        {
            super.paintComponent( g );

            g.drawRect( 0, 0, 29, 29 );
            g.drawString( String.valueOf( mark ), 11, 20 );
        }

    } // end inner-class Square

} // end class TicTacToeClient

/**************************************************************************
 * (C) Copyright 1992-2003 by Deitel & Associates, Inc. and               *
 * Prentice Hall. All Rights Reserved.                                    *
 *                                                                        *
 * DISCLAIMER: The authors and publisher of this book have used their     *
 * best efforts in preparing the book. These efforts include the          *
 * development, research, and testing of the theories and programs        *
 * to determine their effectiveness. The authors and publisher make       *
 * no warranty of any kind, expressed or implied, with regard to these    *
 * programs or to the documentation contained in these books. The authors *
 * and publisher shall not be liable in any event for incidental or       *
 * consequential damages in connection with, or arising out of, the       *
 * furnishing, performance, or use of these programs.                     *
 *************************************************************************/