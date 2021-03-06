package net.bobuhiro11.puzzleroad;

import java.util.ArrayList;
import java.util.Random;

import net.bobuhiro11.puzzleroadconsole.*;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.Toast;

public class  PlayPuzzle{

	private Paint paint,paint_difficulty;
	private Context context;
	
	//スライドの感度,低いほうがよい．
	private int sensitivity=50;	
	private int oldX=-1,oldY=-1;
	
	public Puzzle puzzle;
	
	private Bitmap[] a;
	private Bitmap[] b;
	
	//描画領域(パズル全体)
	public Rect rect;
	//n×nマス
	private int n;
	
	//各マスの領域（汎用的に使う）
	private Rect src,dst;
	
	
	//**移動アニメーション
	//移動する列，行
	private int ani_rawColumn;
	//移動する方向
	private Direction ani_direction;
	//移動量 -1のときは動いていない
	private int ani_moving=-1;
	//1フレーム間で移動量
	private int ani_moving_per_frame;
	
	private MainView mainView;
	
	//スタート，ゴールオブジェクト
	public Person startObject,goalObject;
	
	/**
	 * パズルをするためのViewの一部
	 * @param context コンテキスト
	 * @param rect 描画サイズ
	 * @param n　nマス×nマス
	 * @praam gameNunber 今何ゲーム目か
	 */
	public PlayPuzzle(Context context,MainView mainView,Rect rect,int n,int gameNunber){
		this.context = context;
		this.mainView = mainView;
		
		paint = new Paint();
		paint.setColor(Color.WHITE);
		paint_difficulty = new Paint();
		paint_difficulty.setColor(Color.BLACK);
		int scaledSize = context.getResources().getDimensionPixelSize(R.dimen.myFontSize);
		paint_difficulty.setTextSize(scaledSize);
		
		puzzle = new Puzzle(n+2,1,gameNunber);
		this.rect = rect;
		this.n = n;
		
		this.src = new Rect(0,0,140,140);
		this.dst = new Rect();
		
		Resources r = context.getResources();
		
		a = new Bitmap[8];
        a[0] = BitmapFactory.decodeResource(r, R.drawable.a0);
        a[1] = BitmapFactory.decodeResource(r, R.drawable.a1);
        a[2] = BitmapFactory.decodeResource(r, R.drawable.a2);
        a[3] = BitmapFactory.decodeResource(r, R.drawable.a3);
        a[4] = BitmapFactory.decodeResource(r, R.drawable.a4);
        a[5] = BitmapFactory.decodeResource(r, R.drawable.a5);
        a[6] = BitmapFactory.decodeResource(r, R.drawable.a6);
        a[7] = BitmapFactory.decodeResource(r, R.drawable.a7);
		b = new Bitmap[8];
        b[0] = BitmapFactory.decodeResource(r, R.drawable.b0);
        b[1] = BitmapFactory.decodeResource(r, R.drawable.b1);
        b[2] = BitmapFactory.decodeResource(r, R.drawable.b2);
        b[3] = BitmapFactory.decodeResource(r, R.drawable.b3);
        b[4] = BitmapFactory.decodeResource(r, R.drawable.b4);
        b[5] = BitmapFactory.decodeResource(r, R.drawable.b5);
        b[6] = BitmapFactory.decodeResource(r, R.drawable.b6);
        b[7] = BitmapFactory.decodeResource(r, R.drawable.b7);
        
        this.ani_moving_per_frame = rect.width() / 15;
	}
	
	
	/**
	 * パズル完了時に使う．
	 * スタートからゴールまでの各マスの左上の座標をリストにしたもの．
	 * @return	座標のリスト(スタート，ゴールを含む)
	 */
	public ArrayList<Point> routePosition(){
		ArrayList<Point> list = new ArrayList<Point>();
		int[][] route = puzzle.checkRoute(puzzle.cells, puzzle.start, puzzle.goal);
		int index = 1;
		while(true){
			for(int x=0;x<n+2;x++){
				for(int y=0;y<n+2;y++){
					if(route[x][y] ==index){
						// m番目の座標発見
						int cellWidth = rect.width() / n;
						int cellHeight = rect.height() /n;
						Point p = new Point(
								rect.left + cellWidth*(x-1),
								rect.top  +cellHeight*(y-1));
						list.add(p);
						index++;
						if(x==puzzle.goal.x && y==puzzle.goal.y){
							//ゴールへ到達
							return list;
						}
					}
				}
			}
		}
	}
	
	/**
	 * 落とし穴時に使う．
	 * スタートから落とし穴までの各マスの左上の座標をリストにしたもの．
	 * @return	座標のリスト(スタート，落とし穴を含む)
	 */
	public ArrayList<Point> routePositionHole(){
		ArrayList<Point> list = new ArrayList<Point>();
		Point hole = puzzle.getHolePoint();
		int[][] route = puzzle.checkRoute(puzzle.cells, puzzle.start,hole);
		int index = 1;
		while(true){
			for(int x=0;x<n+2;x++){
				for(int y=0;y<n+2;y++){
					if(route[x][y] ==index){
						// m番目の座標発見
						int cellWidth = rect.width() / n;
						int cellHeight = rect.height() /n;
						Point p = new Point(
								rect.left + cellWidth*(x-1),
								rect.top  +cellHeight*(y-1));
						list.add(p);
						index++;
						if(x==hole.x && y==hole.y){
							//落とし穴へ到達
							return list;
						}
					}
				}
			}
		}
	}
	public void draw(Canvas canvas){
		canvas.drawRect(rect, paint);
		
		int w = rect.width()/n;
		int h = rect.height()/n;
		Cell[][] cells = puzzle.cells;
		int[][] ans = puzzle.checkRoute(puzzle.cells, puzzle.start, puzzle.goal);
		
		//アニメーション(実際はないマスだけどアニメーションには必要)
		if(ani_moving!=-1){
			if(ani_direction==Direction.down){
				dst.set(
						rect.left +ani_rawColumn*w,
						rect.top  -h+ani_moving,
						rect.left +(ani_rawColumn+1)*w,
						rect.top +ani_moving );
				if(ans[ani_rawColumn+1][n]!=0)
					canvas.drawBitmap(a[cells[ani_rawColumn+1][n].toInt()],src, dst, paint);
				else
					canvas.drawBitmap(b[cells[ani_rawColumn+1][n].toInt()],src, dst, paint);
			}else if(ani_direction==Direction.up){
				dst.set(
						rect.left + ani_rawColumn*w,
						rect.bottom -ani_moving,
						rect.left + (ani_rawColumn+1)*w,
						rect.bottom +h -ani_moving );
				if(ans[ani_rawColumn+1][1]!=0)
					canvas.drawBitmap(a[cells[ani_rawColumn+1][1].toInt()],src, dst, paint);
				else
					canvas.drawBitmap(b[cells[ani_rawColumn+1][1].toInt()],src, dst, paint);
			}else if(ani_direction==Direction.right){
				dst.set(
						rect.left-w+ani_moving,
						rect.top + h*ani_rawColumn,
						rect.left +ani_moving,
						rect.top + h*(ani_rawColumn+1));
				if(ans[n][ani_rawColumn+1]!=0)
					canvas.drawBitmap(a[cells[n][ani_rawColumn+1].toInt()],src, dst, paint);
				else
					canvas.drawBitmap(b[cells[n][ani_rawColumn+1].toInt()],src, dst, paint);
			}else if(ani_direction==Direction.left){
				dst.set(
						rect.right -ani_moving,
						rect.top + ani_rawColumn*h,
						rect.right+w-ani_moving,
						rect.top + (ani_rawColumn+1)*h);
				if(ans[1][ani_rawColumn+1]!=0)
					canvas.drawBitmap(a[cells[1][ani_rawColumn+1].toInt()],src, dst, paint);
				else
					canvas.drawBitmap(b[cells[1][ani_rawColumn+1].toInt()],src, dst, paint);
			}
			}
		
		for(int x=0;x<n;x++){
			for(int y=0;y<n;y++){
				
				dst.set(
						rect.left + w*x,
						rect.top  + h*y,
						rect.left + w*(x+1),
						rect.top+ h*(y+1));
				//アニメーション
				if(ani_moving!=-1){
					if(ani_direction==Direction.down && x==ani_rawColumn){
						dst.bottom += ani_moving;
						dst.top+=ani_moving;
					}
					else if(ani_direction==Direction.up && x==ani_rawColumn){
						dst.bottom -= ani_moving;
						dst.top-=ani_moving;
					}
					else if(ani_direction==Direction.right && y==ani_rawColumn){
						dst.right += ani_moving;
						dst.left +=ani_moving;
					}
					else if(ani_direction==Direction.left && y==ani_rawColumn){
						dst.right -= ani_moving;
						dst.left -=ani_moving;
					}
				}
				if(ans[x+1][y+1]!=0)
					canvas.drawBitmap(a[cells[x+1][y+1].toInt()],src, dst, paint);
				else
					canvas.drawBitmap(b[cells[x+1][y+1].toInt()],src, dst, paint);
					
			}
		}
		//あとで消しとく．
		canvas.drawRect(0, rect.bottom, canvas.getWidth(), canvas.getHeight(), paint);
		canvas.drawRect(rect.right, 0, canvas.getWidth(), canvas.getHeight(), paint);
	}
	
	/**
	 * 難易度を描画する．
	 * @param canvas
	 */
	public void draw_difficulty(Canvas canvas){
		canvas.drawText("難易度 : "+puzzle.difficulty,0, 170, paint_difficulty);
	}
	
	/**
	 * 何番目の行かを調べる．
	 * @param oldy
	 * @param newy
	 * @return 何行か０以上，ただし，どの行でもないときは-1を返す．
	 */
	private int checkRaw(int oldy,int newy){
		int h = rect.height()/n;
		//横
		for(int y=0;y<n;y++)
			if( rect.top + h*y<oldy && oldy<rect.top + h*(y+1) &&
				rect.top + h*y<newy && newy<rect.top + h*(y+1)	)
				return y;
		return -1;
	}
	
	/**
	 * 何番目の列かを調べる．
	 * @param oldx
	 * @param newx
	 * @return 何列か０以上，ただし，どの列でもないときは-1を返す．
	 */
	private int checkColumn(int oldx,int newx){
		int w = rect.width()/n;
		//縦
		for(int x=0;x<n;x++)
			if( rect.left + w*x<oldx && oldx<rect.left + w*(x+1) &&
				rect.left + w*x<newx && newx<rect.left + w*(x+1)	)
				return x;
		return -1;
	}

	public void touch(MotionEvent event){
		//if(status==Status.playing){
			switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
				oldX = (int) event.getX();
				oldY = (int) event.getY();
				break;
			case MotionEvent.ACTION_MOVE:
				if(oldX!=-1 && oldY!=-1){
					int raw = this.checkRaw(oldY, (int)event.getY());
					int column = this.checkColumn(oldX, (int)event.getX());
					int dx = (int)event.getX() - oldX;
					int dy = (int)event.getY() - oldY;
					if(dx > sensitivity && raw!=-1){
						//右
						//Log.d("TouchEvent", "right"+raw);
						oldX=-1;
						oldY=-1;

						ani_rawColumn = raw;
						ani_direction = Direction.right;
						ani_moving = 0;
						//puzzle.move(raw+1, Direction.right);
					}else if(dx < -sensitivity && raw!=-1){
						//左
						//Log.d("TouchEvent", "left"+raw);
						oldX=-1;
						oldY=-1;

						ani_rawColumn = raw;
						ani_direction = Direction.left;
						ani_moving = 0;
						//puzzle.move(raw+1, Direction.left);
					}else if(dy > sensitivity && column!=-1){
						//下
						//Log.d("TouchEvent", "down"+column);
						oldX=-1;
						oldY=-1;

						ani_rawColumn = column;
						ani_direction = Direction.down;
						ani_moving = 0;
						//puzzle.move(column+1, Direction.down);
					}else if(dy < -sensitivity && column!=-1){
						//上
						//Log.d("TouchEvent", "up"+column);
						oldX=-1;
						oldY=-1;

						ani_rawColumn = column;
						ani_direction = Direction.up;
						ani_moving = 0;
						//puzzle.move(column+1, Direction.up);
					}
				}
				break;
			}
		//}
	}

	//更新処理
	public void update() {
		//this.startObject.update();
		
		if(ani_moving != -1){
			ani_moving += ani_moving_per_frame;
			if(ani_moving >= rect.width()/n){
				//アニメーション終わり
				ani_moving = -1;
				//実際の移動
				puzzle.move(ani_rawColumn+1, ani_direction);
				//パズル完成
				if(puzzle.isComplete()){
					//辿るべき道順を指定．
					startObject.setPositions(this.routePosition());
					this.mainView.status=Status.personMovin;
				}
				//落とし穴完成
				if(puzzle.isRouteHole()){
					//辿るべき道順を指定．
					startObject.setPositions(this.routePositionHole());
					this.mainView.status=Status.personMovingHole;
				}
			}
		}
		
	}
}
