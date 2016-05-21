package blue.eyes.lamrimreader.core;

/**
 * Created by father on 16/5/19.
 */
public class Dot {
    public int line=-1, index=-1;
    public boolean isSmall=false;
    public char c;

    public Dot(int line, int index, boolean isSmall, char c){
        this.line=line;
        this.index=index;
        this.isSmall=isSmall;
        this.c=c;
    }
}
