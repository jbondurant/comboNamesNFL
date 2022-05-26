import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Scanner;

public class Cell {
    public HashSet<String> comboTeamNames;
    public HashSet<Integer> comboTeamThemes;

    public void setCell(HashSet<String> ctn){
        comboTeamNames = ctn;
        comboTeamThemes = getComboTeamThemes(ctn);
    }

    public Cell(){
        comboTeamNames = new HashSet<String>();
        comboTeamThemes = new HashSet<Integer>();
    }

    public Cell(HashSet<String> ctn){
        comboTeamNames = ctn;
        comboTeamThemes = getComboTeamThemes(ctn);
    }

    public static HashSet<Integer> getComboTeamThemes(HashSet<String> ctns){
        HashSet<Integer> ctt = new HashSet<Integer>();
        ArrayList<HashSet> allThemes = getThemeSets();
        for(int i=0; i<allThemes.size(); i++){
            HashSet<String> currTheme = allThemes.get(i);
            for(String ctn : ctns){
                if(currTheme.contains(ctn)){
                    ctt.add(i);
                }
            }
        }
        return ctt;
    }

    public static ArrayList<HashSet> getThemeSets(){
        Scanner s = null;
        try {
            s = new Scanner(new File("data/themes.txt"));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        ArrayList<HashSet> themeSets = new ArrayList<HashSet>();
        while (s.hasNextLine()){
            String line = s.nextLine();
            String wordsLine = line.split(" ", 2)[1];
            String[] words = wordsLine.split(" ");
            HashSet<String> themeWords = new HashSet<>();
            for(String word : words){
                themeWords.add(word);
            }
            themeSets.add(themeWords);
        }
        s.close();
        return themeSets;
    }
}
