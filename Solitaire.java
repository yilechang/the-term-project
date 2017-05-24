import java.awt.Component ;
import java.awt.Dimension ;
import java.awt.Point ;
import java.awt.Rectangle ;
import java.awt.event.ActionEvent ;
import java.awt.event.ActionListener ;
import java.awt.event.MouseAdapter ;
import java.awt.event.MouseEvent ;
import java.util.Timer ;
import java.util.TimerTask ;

import javax.swing.JButton ;
import javax.swing.JDialog ;
import javax.swing.JEditorPane ;
import javax.swing.JFrame ;
import javax.swing.JOptionPane ;
import javax.swing.JPanel ;
import javax.swing.JScrollPane ;
import javax.swing.JTextField ;

public class Solitaire{
	// SET CONSTANTS
	public static final int TABLE_HEIGHT = Card.CARD_HEIGHT * 6 ;
	public static final int TABLE_WIDTH = (Card.CARD_WIDTH * 7) + 100 ;
	public static final int NUM_FINAL_DECKS = 4 ;
	public static final int NUM_PLAY_DECKS = 7 ;
	public static final Point DECK_POS = new Point(5, 5) ;
	public static final Point SHOW_POS = new Point(DECK_POS.x + Card.CARD_WIDTH + 5, DECK_POS.y) ;
	public static final Point FINAL_POS = new Point(SHOW_POS.x + Card.CARD_WIDTH + 150, DECK_POS.y) ;
	public static final Point PLAY_POS = new Point(DECK_POS.x, FINAL_POS.y + Card.CARD_HEIGHT + 30) ;

	// GAMEPLAY STRUCTURES
	private static FinalStack[] final_cards ;// Foundation Stacks
	private static CardStack[] playCardStack ; // Tableau stacks
	private static final Card newCardPlace = new Card() ;// waste card spot
	private static CardStack deck ; // populated with standard 52 card deck

	// GUI COMPONENTS (top level)
	static final JFrame frame = new JFrame("Klondike Solitaire") ;
	protected static final JPanel table = new JPanel() ;
	//OTHER COMPONENTS
	private static JEditorPane gameTitle = new JEditorPane("text/html", "") ;
	private static JButton showRulesButton = new JButton("Show Rules") ;
	private static JButton newGameButton = new JButton("New Game") ;
	private static JButton toggleTimerButton = new JButton("Pause") ;
	private static JButton toggleTimerButton1 = new JButton("Continue") ;
	private static JTextField scoreBox = new JTextField() ;// displays the score
	private static JTextField timeBox = new JTextField() ;// displays the time
	private static JTextField statusBox = new JTextField() ;// status messages
	private static final Card newCardButton = new Card() ;// reveal waste card

	// TIMER UTILITIES
	private static Timer timer = new Timer() ;
	private static ScoreClock scoreClock = new ScoreClock() ;

	// MISC TRACKING VARIABLES
	private static boolean timeRunning = false ;// timer running?
	private static int score = 0 ;// keep track of the score
	private static int time = 0 ;// keep track of seconds elapsed

	// moves a card to location
	protected static Card moveCard(Card c, int x, int y){
		c.setBounds(new Rectangle(new Point(x, y), new Dimension(Card.CARD_WIDTH + 10, Card.CARD_HEIGHT + 10))) ;
		c.setXY(new Point(x, y)) ;
		return c ;
	}
	
	protected static void setScore(int deltaScore){
		Solitaire.score += deltaScore ;
		String newScore = "Score: " + Solitaire.score ;
		scoreBox.setText(newScore) ;
		scoreBox.repaint() ;
	}

	//game timer utilities
	protected static void updateTimer(){
		Solitaire.time += 1 ;
		// every 10 seconds deduction 2 points
		if (Solitaire.time % 10 == 0){
			setScore(-2) ;
		}
		String time = "Seconds: " + Solitaire.time ;
		timeBox.setText(time) ;
		timeBox.repaint() ;
	}

	protected static void startTimer(){
		scoreClock = new ScoreClock() ;
		// timer update every second
		timer.scheduleAtFixedRate(scoreClock, 1000, 1000) ;
		timeRunning = true ;
	}

	// the pause timer button uses
	protected static void toggleTimer(){
		if (timeRunning && scoreClock != null){
			scoreClock.cancel() ;
			timeRunning = false ;
		} 
		else{
			startTimer() ;
		}
	}
	//use the updatetimmer
	private static class ScoreClock extends TimerTask{
		@Override
		public void run(){
			updateTimer() ;
		}
	}
	
	//monitor the newgame button
	private static class NewGameListener implements ActionListener{
		@Override
		public void actionPerformed(ActionEvent e){
			playNewGame() ;
		}
	}
	//monitor the pause(continue) button
	private static class ToggleTimerListener implements ActionListener{
		@Override
		public void actionPerformed(ActionEvent e){
			toggleTimer() ;
			if(e.getSource()==toggleTimerButton)
				toggleTimerButton.setText("Pause") ;
			if(e.getSource()==toggleTimerButton1)
				toggleTimerButton1.setText("Continue") ;
		}
	}
	
	//monitor the rule button
	private static class ShowRulesListener implements ActionListener{
		@Override
		public void actionPerformed(ActionEvent e){
			JDialog ruleFrame = new JDialog(frame, true) ;
			ruleFrame.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE) ;
			ruleFrame.setSize(TABLE_HEIGHT-400, TABLE_WIDTH-300) ;
			JScrollPane scroll ;
			JEditorPane rulesTextPane = new JEditorPane("text/html", "") ;
			rulesTextPane.setEditable(false) ;
			String rulesText = "<b>Rules</b><br><br>" 
				     + "    您只能移動已翻開的紙牌，並且從大到小，以紅黑花色交錯的方式將紙牌移動到另一已翻開的紙牌上，" 
				     + " 而原本翻開顯示的紙牌移走後，就可將同堆的最上的覆蓋紙牌掀開。另外有 A 紙牌可先移動到右上角當成收集堆" 
				     + " 的基礎，並依照花色收整合四堆，由小排到大的方式收集。如果某個牌堆是空的，則您可以將 K（以及其牌堆中 " 
				     + " 所有的紙牌）移到該牌堆。"      
				     + "<br><br><b> Scoring </b><br><br>" 
				     + "card -> card: 10 points <br> card -> final stack: 10 points <br> cardstack -> card: 5 points <br> K -> space: 5 points <br> cardstack -> final stack: 10 points" 
				     + "<br>10 seconds: -2 points " ;
			rulesTextPane.setText(rulesText) ;
			ruleFrame.add(scroll = new JScrollPane(rulesTextPane)) ;

			ruleFrame.setVisible(true) ;
		}
	}

	//judgement if move the card
	static class CardMovementManager extends MouseAdapter{
		private Card ppCard =null ;
		private Card prevCard = null ;// tracking card for stack
		private Card movedCard = null ;// card moved from stack
		private boolean sourceIsFinalDeck = false ;
		private boolean putBackOnDeck = true ;// used for waste card recycling
		private boolean checkForWin = false ;
		private boolean gameOver = true ;
		private Point start = null ;// where mouse clicked
		private Point stop = null ;// where mouse released
		private Card card = null ; // card to be moved
		private CardStack source = null ;
		private CardStack dest = null ;
		// used for moving a stack of cards
		private CardStack transferStack = new CardStack(false) ;

		private boolean validPlayStackMove(Card source, Card dest){
			//rules
			int s_val = source.getValue().ordinal() ;
			int d_val = dest.getValue().ordinal() ;
			Card.Suit s_suit = source.getSuit() ;
			Card.Suit d_suit = dest.getSuit() ;

			// card should be the more the smaller
			if ((s_val + 1) == d_val){
				// the color of card should be red and black
				switch (s_suit){
					case SPADES:
						if (d_suit != Card.Suit.HEARTS && d_suit != Card.Suit.DIAMONDS)
							return false ;
						else
							return true ;
					case CLUBS:
						if (d_suit != Card.Suit.HEARTS && d_suit != Card.Suit.DIAMONDS)
							return false ;
						else
							return true ;
					case HEARTS:
						if (d_suit != Card.Suit.SPADES && d_suit != Card.Suit.CLUBS)
							return false ;
						else
							return true ;
					case DIAMONDS:
						if (d_suit != Card.Suit.SPADES && d_suit != Card.Suit.CLUBS)
							return false ;
						else
							return true ;
				}
				return false ; //vase
			} 
			else
				return false ;
		}
		//decide whether it's a regular move
		private boolean validFinalStackMove(Card source, Card dest){
			int s_val = source.getValue().ordinal() ;
			int d_val = dest.getValue().ordinal() ;
			Card.Suit s_suit = source.getSuit() ;
			Card.Suit d_suit = dest.getSuit() ;
			 // target must lower
			if (s_val == (d_val + 1)){
				if (s_suit == d_suit)
					return true ;
				else
					return false ;
			} 
			else
				return false ;
		}

		@Override
		// monitor the mouse(press)
		public void mousePressed(MouseEvent e){
			start = e.getPoint() ;
			boolean stopSearch = false ;
			statusBox.setText("") ;
			transferStack.makeEmpty() ;

			for (int x = 0 ; x < NUM_PLAY_DECKS ; x++){
				if (stopSearch)
					break ;
				source = playCardStack[x] ;
				// pinpointing exact card pressed
				for (Component ca : source.getComponents() ){
					Card c = (Card) ca ;
					if (c.getFaceStatus() && source.contains(start)){
						transferStack.putFirst(c) ;
					}
					if (c.contains(start) && source.contains(start) && c.getFaceStatus() ){
						card = c ;
						stopSearch = true ;
						System.out.println("Transfer Size: " + transferStack.showSize() ) ;
						break ;
					}
				}
			}
			// display new card
			if (newCardButton.contains(start) && deck.showSize() > 0){
				if (putBackOnDeck && prevCard != null){
					System.out.println("Putting back on show stack: ") ;
					prevCard.getValue() ;
					prevCard.getSuit() ;
					deck.putFirst(prevCard) ;
				}

				System.out.print("poping deck ") ;
				deck.showSize() ;
				if (prevCard != null){
					table.remove(prevCard) ;
				}					
				Card c = deck.pop().setFaceup() ;
				table.add(Solitaire.moveCard(c, SHOW_POS.x, SHOW_POS.y)) ;
				c.repaint() ;
				table.repaint() ;
				prevCard = c ;
			}

			// preparing to move show card
			if (newCardPlace.contains(start) && prevCard != null){
				movedCard = prevCard ;
			}

			// FINAL (FOUNDATION) CARD OPERATIONS
			for (int x = 0 ; x < NUM_FINAL_DECKS ; x++){

				if (final_cards[x].contains(start)){
					source = final_cards[x] ;
					card = source.getLast() ;
					transferStack.putFirst(card) ;
					sourceIsFinalDeck = true ;
					break ;
				}
			}
			putBackOnDeck = true ;
		}
		

		@Override
		// monitor the mouse(release)
		public void mouseReleased(MouseEvent e){
			stop = e.getPoint() ;
			// used for status bar updates
			boolean validMoveMade = false ;

			// SHOW CARD MOVEMENTS
			if (movedCard != null){
				// Moving from SHOW TO PLAY
				for (int x = 0 ; x < NUM_PLAY_DECKS ; x++){
					dest = playCardStack[x] ;
					// to empty play stack, only kings can go
					if (dest.empty() && movedCard != null && dest.contains(stop)
							&& movedCard.getValue() == Card.Value.KING){
						System.out.print("moving new card to empty spot ") ;
						movedCard.setXY(dest.getXY() ) ;
						table.remove(prevCard) ;
						dest.putFirst(movedCard) ;
						table.repaint() ;
						movedCard = null ;
						putBackOnDeck = false ;
						setScore(5) ;
						validMoveMade = true ;
						break ;
					}
					// play stack
					if (movedCard != null && dest.contains(stop) && !dest.empty() && dest.getFirst().getFaceStatus()
							&& validPlayStackMove(movedCard, dest.getFirst() )){
						System.out.print("moving new card ") ;
						movedCard.setXY(dest.getFirst().getXY() ) ;
						table.remove(prevCard) ;
						dest.putFirst(movedCard) ;
						table.repaint() ;					
						movedCard = null ;
						putBackOnDeck = false ;
						setScore(5) ;
						validMoveMade = true ;
						break ;
					}
				}
				// Moving from SHOW TO FINAL
				for (int x = 0 ; x < NUM_FINAL_DECKS ; x++){
					dest = final_cards[x] ;
					// only aces can go first
					if (dest.empty() && dest.contains(stop)){
						if (movedCard.getValue() == Card.Value.ACE){
							dest.push(movedCard) ;
							table.remove(prevCard) ;
							dest.repaint() ;
							table.repaint() ;
							movedCard = null ;
							putBackOnDeck = false ;
							setScore(10) ;
							validMoveMade = true ;
							break ;
						}
					}
					if (!dest.empty() && dest.contains(stop) && validFinalStackMove(movedCard, dest.getLast() )){
						System.out.println("Destin" + dest.showSize() ) ;
						dest.push(movedCard) ;
						table.remove(prevCard) ;
						dest.repaint() ;
						table.repaint() ;
						movedCard = null ;
						prevCard = null ;
						putBackOnDeck = false ;
						checkForWin = true ;
						setScore(10) ;
						validMoveMade = true ;
						break ;
					}
				}
			}// END SHOW STACK OPERATIONS

			// PLAY STACK OPERATIONS
			if (card != null && source != null){ 
				// Moving from PLAY TO PLAY
				for (int x = 0 ; x < NUM_PLAY_DECKS ; x++){
					dest = playCardStack[x] ;
					// MOVING TO POPULATED STACK
					if (card.getFaceStatus() == true && dest.contains(stop) && source != dest && !dest.empty()
							&& validPlayStackMove(card, dest.getFirst() ) && transferStack.showSize() == 1){
						Card c = null ;
						if (sourceIsFinalDeck)
							c = source.pop() ;
						else
							c = source.popFirst() ;

						c.repaint() ;
						// if is playstack, turn next card up
						if (source.getFirst() != null){
							Card temp = source.getFirst().setFaceup() ;
							temp.repaint() ;
							source.repaint() ;
						}

						dest.setXY(dest.getXY().x, dest.getXY().y) ;
						dest.putFirst(c) ;

						dest.repaint() ;

						table.repaint() ;

						System.out.print("Destination ") ;
						dest.showSize() ;
						if (sourceIsFinalDeck)
							setScore(15) ;
						else
							setScore(10) ;
						validMoveMade = true ;
						break ;
					} 
					else if (dest.empty() && card.getValue() == Card.Value.KING && transferStack.showSize() == 1){
						// MOVING TO EMPTY STACK, ONLY KING ALLOWED
						Card c = null ;
						if (sourceIsFinalDeck)
							c = source.pop() ;
						else
							c = source.popFirst() ;

						c.repaint() ;
						// if playstack, turn next card up
						if (source.getFirst() != null){
							Card temp = source.getFirst().setFaceup() ;
							temp.repaint() ;
							source.repaint() ;
						}

						dest.setXY(dest.getXY().x, dest.getXY().y) ;
						dest.putFirst(c) ;
						dest.repaint() ;
						table.repaint() ;
						System.out.print("Destination ") ;
						dest.showSize() ;
						setScore(5) ;
						validMoveMade = true ;
						break ;
					}
					// Moving STACK of cards from PLAY TO PLAY
					// to EMPTY STACK
					if (dest.empty() && dest.contains(stop) && !transferStack.empty()
							&& transferStack.getFirst().getValue() == Card.Value.KING){
						System.out.println("King To Empty Stack Transfer") ;
						while (!transferStack.empty() ){
							System.out.println("popping from transfer: " + transferStack.getFirst().getValue() ) ;
							dest.putFirst(transferStack.popFirst() ) ;
							source.popFirst() ;
						}
						if (source.getFirst() != null){
							Card temp = source.getFirst().setFaceup() ;
							temp.repaint() ;
							source.repaint() ;
						}

						dest.setXY(dest.getXY().x, dest.getXY().y) ;
						dest.repaint() ;
						table.repaint() ;
						setScore(5) ;
						validMoveMade = true ;
						break ;
					}
					// to POPULATED STACK
					if (dest.contains(stop) && !transferStack.empty() && source.contains(start)
							&& validPlayStackMove(transferStack.getFirst(), dest.getFirst() )){
						System.out.println("Regular Stack Transfer") ;
						while (!transferStack.empty() ){
							System.out.println("popping from transfer: " + transferStack.getFirst().getValue() ) ;
							dest.putFirst(transferStack.popFirst() ) ;
							source.popFirst() ;
						}
						if (source.getFirst() != null){
							Card temp = source.getFirst().setFaceup() ;
							temp.repaint() ;
							source.repaint() ;
						}

						dest.setXY(dest.getXY().x, dest.getXY().y) ;
						dest.repaint() ;
						table.repaint() ;
						setScore(5) ;
						validMoveMade = true ;
						break ;
					}
				}
				// from PLAY TO FINAL
				for (int x = 0 ; x < NUM_FINAL_DECKS ; x++){
					dest = final_cards[x] ;

					if (card.getFaceStatus() == true && source != null && dest.contains(stop) && source != dest){
						// TO EMPTY STACK
						if (dest.empty() ){
							// empty final should only take an ACE
							if (card.getValue() == Card.Value.ACE){
								Card c = source.popFirst() ;
								c.repaint() ;
								if (source.getFirst() != null){
									Card temp = source.getFirst().setFaceup() ;
									temp.repaint() ;
									source.repaint() ;
								}

								dest.setXY(dest.getXY().x, dest.getXY().y) ;
								dest.push(c) ;
								dest.repaint() ;
								table.repaint() ;

								System.out.print("Destination ") ;
								dest.showSize() ;
								card = null ;
								setScore(10) ;
								validMoveMade = true ;
								break ;
							}// TO POPULATED STACK
						} 
						else if (validFinalStackMove(card, dest.getLast() )){
							Card c = source.popFirst() ;
							c.repaint() ;
							if (source.getFirst() != null){
								Card temp = source.getFirst().setFaceup() ;
								temp.repaint() ;
								source.repaint() ;
							}

							dest.setXY(dest.getXY().x, dest.getXY().y) ;
							dest.push(c) ;
							dest.repaint() ;
							table.repaint() ;

							System.out.print("Destination ") ;
							dest.showSize() ;
							card = null ;
							checkForWin = true ;
							setScore(10) ;
							validMoveMade = true ;
							break ;
						}
					}
				}
			}// end cycle through play decks

			// SHOWING STATUS MESSAGE IF MOVE INVALID
			if (!validMoveMade && dest != null && card != null){
				statusBox.setText("That Is Not A Valid Move") ;
			}

			if (checkForWin){
				boolean gameNotOver = false ;
				// cycle through final decks, if they're all full then game over
				for (int x = 0 ; x < NUM_FINAL_DECKS ; x++){
					dest = final_cards[x] ;
					if (dest.showSize() != 12){
						// one deck is not full, so game is not over
						gameNotOver = true ;
						break ;
					}
				}
				if (!gameNotOver)
					gameOver = true ;
			}

			if (checkForWin && gameOver){
				scoreClock.cancel(); 
			    timeRunning = false;
				JOptionPane.showMessageDialog(table, "Congratulations! You've Won!") ;
				statusBox.setText("Game Over!") ;
			}
			// RESET
			start = null ;
			stop = null ;
			source = null ;
			dest = null ;
			card = null ;
			sourceIsFinalDeck = false ;
			checkForWin = false ;
			gameOver = false ;
		}// end mousePressed()
	}

	static void playNewGame(){
		deck = new CardStack(true) ; // deal 52 cards
		deck.shuffle() ;
		table.removeAll() ;
		// reset stacks if user starts a new game in the middle of one
		if (playCardStack != null && final_cards != null){
			for (int x = 0 ; x < NUM_PLAY_DECKS ; x++){
				playCardStack[x].makeEmpty() ;
			}
			for (int x = 0 ; x < NUM_FINAL_DECKS ; x++){
				final_cards[x].makeEmpty() ;
			}
		}
		// 4 boxes on top
		final_cards = new FinalStack[NUM_FINAL_DECKS] ;
		for (int x = 0 ; x < NUM_FINAL_DECKS ; x++){
			final_cards[x] = new FinalStack() ;

			final_cards[x].setXY((FINAL_POS.x + (x * Card.CARD_WIDTH)) + 10, FINAL_POS.y) ;
			table.add(final_cards[x]) ;

		}
		// place new card distribution button
		table.add(moveCard(newCardButton, DECK_POS.x, DECK_POS.y)) ;
		// initialize & place play (tableau) decks/stacks
		playCardStack = new CardStack[NUM_PLAY_DECKS] ;
		for (int x = 0 ; x < NUM_PLAY_DECKS ; x++){
			playCardStack[x] = new CardStack(false) ;
			playCardStack[x].setXY((DECK_POS.x + (x * (Card.CARD_WIDTH + 10))), PLAY_POS.y) ;

			table.add(playCardStack[x]) ;
		}

		//new game
		for (int x = 0 ; x < NUM_PLAY_DECKS ; x++){
			Card c = deck.pop().setFaceup() ;
			playCardStack[x].putFirst(c) ;

			for (int y = x + 1 ; y < NUM_PLAY_DECKS ; y++){
				playCardStack[y].putFirst(c = deck.pop() ) ;
				
			}
			
		}
		
		time = 0 ;
		scoreClock.cancel();

		newGameButton.addActionListener(new NewGameListener() ) ;
		newGameButton.setBounds(0, TABLE_HEIGHT - 70, 130, 30) ;

		showRulesButton.addActionListener(new ShowRulesListener() ) ;
		showRulesButton.setBounds(130, TABLE_HEIGHT - 70, 130, 30) ;

		
		gameTitle.setText("<b>期末報告</b> <br> Produced By <br> 施鈺宣 405411256 <br> 楊以萱 405410142 <br> 張以勒 405411439  <br><b>  指導教授:鐘興臺</b>");
		gameTitle.setEditable(false) ;
		gameTitle.setOpaque(false) ;
		gameTitle.setBounds(655, TABLE_HEIGHT - 220, 200, 200) ;

		scoreBox.setBounds(655, TABLE_HEIGHT - 70, 130, 30) ;
		scoreBox.setText("Score: 0") ;
		scoreBox.setEditable(false) ;
		scoreBox.setOpaque(false) ;

		timeBox.setBounds(520, TABLE_HEIGHT - 70, 135, 30) ;
		timeBox.setText("Seconds: 0") ;
		timeBox.setEditable(false) ;
		timeBox.setOpaque(false) ;

		startTimer() ;

		toggleTimerButton.setBounds(260, TABLE_HEIGHT - 70, 130, 30) ;
		toggleTimerButton.addActionListener(new ToggleTimerListener() ) ;
		
		toggleTimerButton1.setBounds(390, TABLE_HEIGHT - 70, 130, 30) ;
		toggleTimerButton1.addActionListener(new ToggleTimerListener() ) ;

		statusBox.setBounds(300, TABLE_HEIGHT - 150, 180, 50) ;
		statusBox.setEditable(false) ;
		statusBox.setOpaque(false) ;
		
		

		table.add(statusBox) ;
		table.add(toggleTimerButton) ;
		table.add(toggleTimerButton1) ;
		table.add(gameTitle) ;
		table.add(timeBox) ;
		table.add(newGameButton) ;
		table.add(showRulesButton) ;
		table.add(scoreBox) ;
		table.repaint() ;
	}

	
	
}