package ishizakiStrategies;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import game.*;

//１手だけ先読みするシンプルな戦略。評価関数は石を置けるマスの数（手数）と角のみを考慮。
public class IshizakiNoSore extends Strategy {
	Move bestMove = new Move();
	int playnum = 0;
	int notplaynum = 0;
	final int MAX_VALUE = 64000;
	final int First = 64;//残り何マスまで定石を打つか
	final int MidDepth = 4;//通常何手まで読むか
	final int Last = 5;//残り何マスで完全読を始めるか
	final static int PenaltyCorner = 100; //相手に角をとられてしまう戦略に対するペナルティー
	final static int corners[][] = {{0,0}, {0, GameMaster.SIZE-1}, {GameMaster.SIZE-1,0}, {GameMaster.SIZE-1,GameMaster.SIZE-1}}; //角の位置

	public IshizakiNoSore(Player _thisPlayer, int size) { //コンストラクタ
		super(_thisPlayer, size);
	}

	@Override //親クラスgame.Strategyの抽象メソッドを実装
	public Move nextMove(GameState currentState, int remainingTime){ 
		if(thisPlayer==Player.Black){
			playnum = 1;
			notplaynum = -1;
		}else{
			playnum = -1;
			notplaynum = 1;
		}
		int left;
		left = currentState.getNumberOfAvailableCells();
		if(left > First){
			//joseki();
		}else if(left < Last){
			System.out.println("LAST");
			mid(currentState, Last, thisPlayer,-1,-1,true);;
		}else{
			System.out.println("MID");
			mid(currentState, this.MidDepth, thisPlayer,-1,-1,true);
		}
		if(left <= 2){
			if(currentState.numAvailable(thisPlayer)==1){
				int a = 0;
				try {
						a = currentState.nextState(thisPlayer, bestMove.x, bestMove.y).numPieces(thisPlayer);
				} catch (OthelloMoveException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				File file = new File("test");
				try {
					PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(file)));
					pw.print(a);
					pw.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				//System.out.println(a+"dayo");
			}
		}
		return bestMove;
	}
	private int mid(GameState currentState, int in_depth, Player player, int x, int y, boolean flag){
		boolean first = false;
		int left = currentState.getNumberOfAvailableCells();
		int	max = -MAX_VALUE;
		int premax = -MAX_VALUE;
		if(currentState.inGame == false){
			int score = currentState.numPieces(thisPlayer) - currentState.numPieces(thisPlayer.oppositePlayer());
			return -score;
		}else if(in_depth == 0){
			int score = 0;
			if(left < Last){
				score = computeScore2(currentState);
			}else{
				score = computeScore(currentState);
			}
			//System.out.println("socore = "+score);
			return -score;
		}
		boolean Check = false;
		for (int i = 0; i < SIZE; i++) {
			for (int j = 0; j < SIZE; j++) {
				if(currentState.isLegal(player,i,j)){
					try {
						Check = true;
						GameState expectedState = currentState.nextState(player, i, j);
						if(x==-1 || first == true){
							//System.out.println("aaaaa i;j = "+i+";"+j );
							x = i;
							y = j;
							first = true;
							premax = mid(expectedState, in_depth-1, player.oppositePlayer(),i,j,false);
						}else{
							premax = mid(expectedState, in_depth-1, player.oppositePlayer(),x,y,false);
						}
						//System.out.println("premax"+premax);
						if(max < premax){
							max = premax;
							if(first){
								//System.out.println("max = "+max+" x = "+x+" y = "+y);
								bestMove.x = x;
								bestMove.y = y;
							}
						}
					} catch (OthelloMoveException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		}
		if(Check == false){//パスの場合
		//	System.out.println("dbg");
			for (int i = 0; i < SIZE; i++) {
				for (int j = 0; j < SIZE; j++) {
					if(currentState.isLegal(player.oppositePlayer(),i,j)){
						try {
							GameState expectedState = currentState.nextState(player.oppositePlayer(), i, j);
							if(x==-1 && y == -1 || first == true){
								x = i;
								y = j;
								first = true;
								premax = mid(expectedState, in_depth-1, player,i,j,true);
							}else{
								premax = mid(expectedState, in_depth-1, player,x,y,true);
							}
							if(max < premax){
								max = premax;
								if(first){
									//System.out.println("max = "+max);
									bestMove.x = x;
									bestMove.y = y;
								}
							}
						} catch (OthelloMoveException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}
			}
		}
		//System.out.println("dep = "+in_depth+"  "+-max);
		return -max;
	}
	
	//盤面のスコアを評価するメソッド
	private int computeScore(GameState expectedState){
		int scoreMaxAvailable =  expectedState.numAvailable(thisPlayer) - expectedState.numAvailable(thisPlayer.oppositePlayer()); //自分が石を置けるマスの数 - 相手が石を置けるマスの数
		int scoreCorner1 = 0;
		int scoreCorner2 = 0;
		for(int i=0; i<4; i++){ //もし次の盤面で相手が角に石を置けるなら、その評価値をPenaltyCornerだけ減じる
			//if(expectedState.isLegal(thisPlayer.oppositePlayer(), corners[i][0], corners[i][1])){
				//scoreCorner -= PenaltyCorner;
			//}
			if(expectedState.cells[corners[i][0]][corners[i][1]].getValue()==notplaynum){
				scoreCorner1 -= 100;
			}
			if(expectedState.cells[corners[i][0]][corners[i][1]].getValue()==playnum){
				scoreCorner2 += 100;
			}
			//System.out.println(thisPlayer);
		}
	
		return scoreMaxAvailable+scoreCorner1+scoreCorner2;
	}
	private int computeScore2(GameState expectedState){
		int scoreMaxAvailable =  expectedState.numPieces(thisPlayer) - expectedState.numPieces(thisPlayer.oppositePlayer()); 
		return scoreMaxAvailable;
	}

}