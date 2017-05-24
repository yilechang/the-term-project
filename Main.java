import java.awt.Color ;
import java.awt.Container ;
import javax.swing.JFrame ;

public class Main extends Solitaire{
	public static void main(String[] args){

		Container contentPane ;

		frame.setSize(TABLE_WIDTH,TABLE_HEIGHT) ;

		Solitaire.table.setLayout(null) ;
		Solitaire.table.setBackground(new Color(0, 180, 0)) ;

		contentPane = frame.getContentPane() ;
		contentPane.add(Solitaire.table) ;
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE) ;

		playNewGame() ;
		
		table.addMouseListener(new CardMovementManager() ) ;
		table.addMouseMotionListener(new CardMovementManager() ) ;

		frame.setVisible(true) ;
	
	}
}
