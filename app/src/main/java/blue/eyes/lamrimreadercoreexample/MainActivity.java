package blue.eyes.lamrimreadercoreexample;

import android.graphics.Color;
import android.media.MediaPlayer;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

import blue.eyes.lamrimreader.core.BookMapUtil;
import blue.eyes.lamrimreader.core.Dot;
import blue.eyes.lamrimreader.core.LinearIndex;
import blue.eyes.lamrimreader.core.PLIndex;
import blue.eyes.lamrimreader.core.Subtitle;
import blue.eyes.lamrimreader.core.SubtitleUtil;
import blue.eyes.lamrimreader.core.TheoryParseListener;
import blue.eyes.lamrimreader.core.TheoryUtil;
import blue.eyes.lamrimreadercoreexample.data.BookMap;
import blue.eyes.lamrimreadercoreexample.data.TheoryData;

public class MainActivity extends AppCompatActivity {
    int mediaIndex=50; /* 026A */
    EditText searchInput;
    Button lastPageBtn, nextPageBtn, searchLastBtn, searchNextBtn;
    ImageButton playBtn, rewBtn, ffBtn;
    TheoryTextView pageView=null;
    TextView pageNum, subtitleView, timeView, infoView;
    MediaPlayer mediaPlayer=null;

    int pageIndex=47;
    TheoryUtil tu=null;
    PLIndex highlightMark =null, searchIndex=new PLIndex(0,0,-1);
    PLIndex[] bookMap=null;
    TheoryParseListener listener = null;
    String lastSearchString=null;
    Subtitle[] subtitles;
    CheckThread chkThread=null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        pageView = (TheoryTextView)findViewById(R.id.theoryView);
        pageNum=(TextView) findViewById(R.id.pageNum);
        subtitleView=(TextView) findViewById(R.id.subtitleView);
        timeView=(TextView) findViewById(R.id.timeView);
        infoView=(TextView) findViewById(R.id.infoView);
        lastPageBtn=(Button)findViewById(R.id.lastPageBtn);
        nextPageBtn=(Button)findViewById(R.id.nextPageBtn);
        searchLastBtn=(Button)findViewById(R.id.searchLast);
        searchNextBtn=(Button)findViewById(R.id.searchNext);
        rewBtn=(ImageButton)findViewById(R.id.rewBtn);
        playBtn=(ImageButton)findViewById(R.id.playBtn);
        ffBtn=(ImageButton)findViewById(R.id.ffBtn);
        searchInput=(EditText)findViewById(R.id.searchInput);

        tu=new TheoryUtil(TheoryData.content);
        subtitles= SubtitleUtil.loadSubtitle(getResources().openRawResource(R.raw.srt026a));

        int[][] mediaBookMaps= BookMapUtil.getMaps(BookMap.mapStr[mediaIndex]);

        if(mediaBookMaps!=null){
            bookMap=new PLIndex[subtitles.length]; // For setHighlightWord(int startPage, int line, int startIndex, int length)

            for(int i=0;i<mediaBookMaps.length;i++){
                int index=mediaBookMaps[i][1];
                bookMap[index]=new PLIndex(mediaBookMaps[i][BookMapUtil.PAGE], mediaBookMaps[i][BookMapUtil.LINE], mediaBookMaps[i][BookMapUtil.WORD], mediaBookMaps[i][BookMapUtil.LENGTH]);
            }

            PLIndex last=null;
            for(int i=0;i<bookMap.length;i++){
                if(bookMap[i]!=null){
                    last=bookMap[i];
                }
                if(last==null)continue;
                bookMap[i]=last;
            }
        }

        lastPageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(getClass().getName(),"Index="+pageIndex);
                if(pageIndex==0)return;
                showPage(--pageIndex);
            }
        });

        nextPageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(getClass().getName(),"Index="+pageIndex);
                if(pageIndex==TheoryData.content.length-1)return;
                showPage(++pageIndex);
            }
        });

        playBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mediaPlayer.isPlaying()){
                    playBtn.setImageResource(android.R.drawable.ic_media_play);
                    mediaPlayer.pause();
                    chkThread.stopRun();
                }
                else {
                    playBtn.setImageResource(android.R.drawable.ic_media_pause);
                    mediaPlayer.start();
                    chkThread=new CheckThread();
                    chkThread.start();
                }
            }
        });

        rewBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int pos=mediaPlayer.getCurrentPosition();
                int index=SubtitleUtil.subtitleBSearch(subtitles, pos)-1;
                if(index<0)index=0;
                mediaPlayer.seekTo(subtitles[index].startTimeMs);
//                Toast.makeText(MainActivity.this, "設定播放位置為第"+index+"個字幕位置，時間為"+SubtitleUtil.getMsToHMS(subtitles[index].startTimeMs),Toast.LENGTH_LONG).show();
            }
        });

        ffBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int pos=mediaPlayer.getCurrentPosition();
                int index=SubtitleUtil.subtitleBSearch(subtitles, pos)+1;
                if(index>=subtitles.length)index=subtitles.length-1;
                mediaPlayer.seekTo(subtitles[index].startTimeMs);
//                Toast.makeText(MainActivity.this, "設定播放位置為第"+index+"個字幕位置，時間為"+SubtitleUtil.getMsToHMS(subtitles[index].startTimeMs),Toast.LENGTH_LONG).show();
            }
        });

        searchLastBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String str=searchInput.getText().toString();
                if(str.isEmpty())return;

                if(str.equals(lastSearchString)){
                    searchIndex.index--;
                }
                else{
                    searchIndex.page=pageIndex;
                    searchIndex.line=0;
                    searchIndex.line=-1;
                }

                Log.d(getClass().getName(),"Search ["+str+"] from page "+searchIndex.page+", line "+searchIndex.line+", index "+searchIndex.line);
                PLIndex loc=tu.searchLast(searchIndex,str);
                if(loc == null){
                    Toast.makeText(MainActivity.this, "找不到"+str,Toast.LENGTH_LONG).show();
                    return;
                }
                searchIndex=loc;
                pageIndex=loc.page;
                showPage(pageIndex);
                lastSearchString=str;
                highlightMark =new PLIndex(loc.page, loc.line, loc.index, str.length());
                highlightText();

                Toast.makeText(MainActivity.this, "位於第"+(loc.page+1)+"頁, 第"+(loc.line+1)+"行, 第"+(loc.index+1)+"字",Toast.LENGTH_SHORT).show();
            }
        });

        searchNextBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String str=searchInput.getText().toString();
                if(str.isEmpty())return;

                if(str.equals(lastSearchString)){
                    searchIndex.index++;
                }
                else{
                    searchIndex.page=pageIndex;
                    searchIndex.line=0;
                    searchIndex.index=-1;
                }

                Log.d(getClass().getName(),"Search ["+str+"] from page "+searchIndex.page+", line "+searchIndex.line+", index "+searchIndex.index);
                PLIndex loc=tu.searchNext(searchIndex,str);
                if(loc == null){
                    Toast.makeText(MainActivity.this, "找不到["+str+"]",Toast.LENGTH_LONG).show();
                    return;
                }
                searchIndex=loc;
                pageIndex=loc.page;
                showPage(pageIndex);
                lastSearchString=str;
                highlightMark =new PLIndex(loc.page, loc.line, loc.index, str.length());
                highlightText();

                Toast.makeText(MainActivity.this, "位於第"+(loc.page+1)+"頁, 第"+(loc.line+1)+"行, 第"+(loc.index+1)+"字",Toast.LENGTH_SHORT).show();
            }
        });

        mediaPlayer=MediaPlayer.create(MainActivity.this, R.raw.mp3026a);
        Log.d(getClass().getName(),"Show page "+pageIndex);
        showPage(pageIndex);

    }

    @Override
    public void onBackPressed()
    {
        if(chkThread!=null)chkThread.stopRun();
        super.onBackPressed();
        mediaPlayer.release();

    }



    public void showPage(int num){
        if(listener == null) {
            final float smallSize=0.9f;
            listener = new TheoryParseListener() {
                @Override
                public void onSegmentFound(int line, int index, int length, String segText, boolean isBold, boolean isNum, boolean isSmall) {
                    Log.d(getClass().getName(), "Found segment =" + segText);
                    SpannableString str = new SpannableString(segText);
                    //str.setSpan(new ForegroundColorSpan(textColor), 0, str.length(), Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
                    if (isBold) {
                        Log.d(getClass().getName(), "Set bold.");
                        str.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), 0, str.length(), Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
//                    str.setSpan(new ForegroundColorSpan(boldColor), 0, str.length(), Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
                    }
                    if (isNum) {
                        Log.d(getClass().getName(), "Set num.");
                        str.setSpan(new ForegroundColorSpan(Color.rgb(0, 0, 255)), 0, str.length(), Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
                    }
                    if (isSmall) {
                        Log.d(getClass().getName(), "Set small.");
                        str.setSpan(new RelativeSizeSpan(smallSize), 0, str.length(), Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
                    }
                    pageView.append(str);
                }

                @Override
                public void onNewLineFound(int line, int index) {
                    Log.d(getClass().getName(), "Found new line");
                    pageView.append("\n");
                }

                @Override
                public void onFinishParse(ArrayList<Dot> dotList) {
                    if(highlightMark != null && pageIndex == highlightMark.page)
                        highlightText();
                    pageView.drawDot(dotList);
                }
            };
        }

        pageView.setPageIndex(num);
        pageView.setText("");
        pageView.drawDot(null);
        tu.parsePage(num,listener);
        pageView.highlightText();
        pageNum.setText(""+(num+1));
    }

    private void highlightText(){
        LinearIndex[] marks=tu.getHighlightMark(highlightMark);
        pageView.setHighlightMarks(marks);
    }


    class CheckThread extends Thread{
        boolean isStop=false;

        public void stopRun(){isStop=true;}
        @Override
        public void run() {
            while (!isStop) {
                final int pos=mediaPlayer.getCurrentPosition();
                final int index=SubtitleUtil.subtitleBSearch(subtitles, pos);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        subtitleView.setText(subtitles[index].text);
                        timeView.setText(SubtitleUtil.getMsToHMS(pos,",","\"",false));
                        PLIndex map=bookMap[index];
                            if(pageIndex==map.page)
                                if(highlightMark == null || !(highlightMark.equals(map))) {
                                    highlightMark = map;
                                    highlightText();
                                }
                    }
                });


                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    return;
                }
            }
        }
    }
}
