package blue.eyes.lamrimreadercoreexample;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.BackgroundColorSpan;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.TextView;

import java.util.ArrayList;

import blue.eyes.lamrimreader.core.Dot;
import blue.eyes.lamrimreader.core.LinearIndex;

/**
 * Created by father on 16/5/16.
 */
public class TheoryTextView extends TextView {
    Context context = null;
    int pageIndex=-1;
    ArrayList<Dot> dotList = null;
    LinearIndex[] highlightMarks=null;
    float smallTextSizeRate=0.9f;

    public TheoryTextView(Context context) {
        super(context);
        this.context=context;
    }

    public TheoryTextView(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        this.context=context;
    }

    public TheoryTextView(Context context, AttributeSet attrs, int defStyle)
    {
        super(context, attrs, defStyle);
        this.context=context;
    }

    public void drawDot(ArrayList<Dot> dotList){
        this.dotList=dotList;
        invalidate();
    }

    public void setPageIndex(int i){
        this.pageIndex=i;
    }

    public int getPageIndex(){
        return pageIndex;
    }

    public void setHighlightMarks(LinearIndex[] marks){
        this.highlightMarks=marks;
        highlightText();
    }

    public void highlightText(){
        if(highlightMarks==null)return;

        for(LinearIndex li:highlightMarks){
            if(pageIndex == li.page)
                highlightText(li.index, li.length);
        }
    }


    private void highlightText(int startIndex, int length){
        Log.d(getClass().getName(),"Highlight index "+startIndex+", length "+length);
        int color=Color.CYAN;
        SpannableStringBuilder text=new SpannableStringBuilder(getText().toString());
        String str=text.toString();
        int invalidStrCount=0;
        if(startIndex+length>str.length())length=str.length()-startIndex;//某些狀況下標記的字元會跨二頁，造成OutOfBoundException，故檢查長度使其不超過邊界

        //計算範圍內還有多少不可顯示字元
        for(int i=startIndex;i<startIndex+length;i++){
            if(str.charAt(i)=='\n')
                invalidStrCount++;
        }

        int strLen=length+invalidStrCount;
        text.setSpan(new BackgroundColorSpan(color), startIndex, startIndex+strLen, Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
        super.setText(text);
        Log.d(getClass().getName(),"設定標記文字 ==> "+text.toString().substring(startIndex,startIndex+strLen));
    }


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (canvas == null) return;
        if (dotList == null || dotList.size()==0)return;

        float orgTextSize=getTextSize();
        int pointSize=(int) (getTextSize()/7);
        int yShift=(int) (getTextSize()/5);
        Paint paint=getPaint();

        String[] lineContent=getText().toString().split("\n");
        for(Dot d:dotList){
            Rect rect=new Rect();
            int y=getLineBounds(d.line, rect);  // 取得該行的座標基準位置

            if(d.isSmall)paint.setTextSize(orgTextSize * smallTextSizeRate);  //若此行是標題小字，則將字形設定為小字字型，以利後續計算
            if(d.c=='。'){   //此符號是句號則設定為空點。
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(2);
            }
            else    //此符號是逗號，設定為實點
                paint.setStyle(Paint.Style.FILL);

            //取得該字的X座標偏移量
            float x=rect.left+(paint.measureText(lineContent[d.line],0,d.index));
//			Log.d("TheoryPageView","Draw dot: line: "+(d.line+1)+", index: "+d.index+", x="+x+", y="+(y+yShift)+", radius="+pointSize);
            canvas.drawCircle(x, y+yShift, pointSize, paint);

            getPaint().setTextSize(orgTextSize);
            paint.setStyle(Paint.Style.FILL);
        }
    }
}
