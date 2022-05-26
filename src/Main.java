import com.mongodb.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Main {
    //hmm now is there a way to make 16 pairs of 2 for a total of 32 distinct teams
    public static ArrayList<String> teams = getTeams();
    public static HashSet<String> englishWords =getWords("data/englishWords.txt");
    public static HashSet<String> badWords = getWords("data/badWords.txt");
    public static HashSet<String> manRemWords = getWords("data/manuallyRemovedWords.txt");
    public static HashSet<String> manAppWords = getWords("data/manAppWords.txt");
    public static HashSet<String> selfMergeWords = getWords("data/selfMerges.txt");
    public static HashSet<String> englishNouns = getEnglishNouns();
    public static ArrayList<String> themes = getThemes();
    public static HashMap<String, HashSet<String>> themesToWords = getThemesToWords();

    public static void main(String[] args) throws IOException {
        MongoClient mongoClient = new MongoClient();
        DB database = mongoClient.getDB("MergersNFL");
        DBCollection collection = database.getCollection("Leagues");




        for(int i=0; i<3; i++) {
            League l = getValidLeague();
            int random_int = (int)Math.floor(Math.random()*(10000)+ 1);

            DBObject leagueObject = new BasicDBObject("_id", random_int);
            for(Division d : l.divisions){
                String divName = d.name;
                DBObject divisionObject = new BasicDBObject("name", divName);
                for(int j=0; j<d.teams.size(); j++){
                    Team t = d.teams.get(j);
                    DBObject teamObject = new BasicDBObject("comboName", t.comboName)
                            .append("team a", t.teamA)
                            .append("team b", t.teamB);
                    ((BasicDBObject) divisionObject).append(t.comboName, teamObject);
                }
                ((BasicDBObject) leagueObject).append(divName, divisionObject);
            }
            collection.insert(leagueObject);

        }
    }

    public static League getValidLeague() throws IOException {
        Cell[][] validComboTeams = getComboTeams();
        while(true) {
            int[][] cellSampleLocations = getSampleLocations(/*forcedLocations*/);
            if(hasEmptyCell(validComboTeams, cellSampleLocations)) {
                continue;
            }
            League league = arrangementFound(validComboTeams, cellSampleLocations);
            if(league != null){
                league.PrintLeague();
                return league;
            }
        }

    }

    public static String getComboTeamFromThemeAndNames(String theme, HashSet<String> names){
        HashSet<String> possibleWords = themesToWords.get(theme);
        for(String name : names){
            if(possibleWords.contains(name)){
                return name;
            }
        }
        return "error";
    }

    public static String getTeamName(int teamNum){
        return teams.get(teamNum);
    }

    public static String getThemeName(int themeNum){
        return themes.get(themeNum);
    }

    private static League arrangementFound(Cell[][] validComboTeams, int[][] cellSampleLocations) {
        ArrayList<Integer> allNumbers = new ArrayList<>();
        HashSet<String> usedComboTeamNames = new HashSet<>();

        for(int i=0; i<cellSampleLocations.length; i++){
            Cell cell = validComboTeams[cellSampleLocations[i][0]][cellSampleLocations[i][1]];
            allNumbers.addAll(cell.comboTeamThemes);
        }
        Map<Integer, Long> allNumbersCount = allNumbers.stream().collect(Collectors.groupingBy(Function.identity(),Collectors.counting()));
        ArrayList<Integer> themesAtLeast4 = new ArrayList<>();
        for(Integer theme : allNumbersCount.keySet()){
            int count = allNumbersCount.get(theme).intValue();
            if(count >= 4){
                themesAtLeast4.add(theme);
            }
        }
        //System.out.println(themesAtLeast4.size());
        if(themesAtLeast4.size() < 4){
            return null;
        }
        Collections.shuffle(themesAtLeast4);

        ArrayList<Division> allDivisions = new ArrayList<>();//jt
        HashMap<Integer, Integer> fourThemesCount = new HashMap<>();
        for(int i=0; i<4; i++){
            fourThemesCount.put(themesAtLeast4.get(i), 0);
            String themeName = getThemeName(themesAtLeast4.get(i));
            Division division = new Division(themeName);
            allDivisions.add(division);
        }
        for(int i=0; i<cellSampleLocations.length; i++){
            Cell cell = validComboTeams[cellSampleLocations[i][0]][cellSampleLocations[i][1]];
            String teamA = getTeamName(cellSampleLocations[i][0]);//jt
            String teamB = getTeamName(cellSampleLocations[i][1]);//jt

            //i still need a get comboteamName from cell and themeNumber
            for(Integer theme : fourThemesCount.keySet()){
                if(fourThemesCount.get(theme) >= 4){
                    continue;
                }
                if(cell.comboTeamThemes.contains(theme)){
                    String currThemeName = getThemeName(theme);//jt
                    for(Division div : allDivisions){
                        if(div.name.equals(currThemeName)){
                            String comboTeamAB = getComboTeamFromThemeAndNames(currThemeName, cell.comboTeamNames);
                            if(usedComboTeamNames.contains(comboTeamAB)){
                                return null;
                            }
                            usedComboTeamNames.add(comboTeamAB);
                            Team teamToInsert = new Team(comboTeamAB, teamA, teamB);//jt
                            div.teams.add(teamToInsert);
                        }
                    }

                    fourThemesCount.put(theme, fourThemesCount.get(theme) + 1);
                    break;
                }
            }
        }
        for(int count : fourThemesCount.values()){
            if(count < 4){
                return null;
            }
        }
        League league = new League(allDivisions);//jt
        return league;
    }


    public static boolean hasEmptyCell(Cell[][] validComboTeams, int[][] cellSampleLocations) {
        for(int i=0; i<cellSampleLocations.length; i++){
            Cell cell = validComboTeams[cellSampleLocations[i][0]][cellSampleLocations[i][1]];
            if(cell.comboTeamThemes.size() == 0){
                return true;
            }
        }
        return false;
    }

    public static int[][] getSampleLocations(/*int [][] forcedLocations*/){
        /*for(int i=0; i<forcedLocations.length; i++){
            for(int j=0; j<forcedLocations[i].length; j++){

            }
        }*/



        int[][] sampleLocations = new int[16][2];
        ArrayList<Integer> nums0To31 = new ArrayList<>();
        for(int i=0; i<32; i++){
            nums0To31.add(i);
        }

        int indexToRemove = 31;
        Collections.shuffle(nums0To31);
        for(int i=0; i<16; i++){
            sampleLocations[i][0] =nums0To31.remove(indexToRemove);
            sampleLocations[i][1] = nums0To31.remove(indexToRemove - 1);
            indexToRemove -= 2;
        }


        return sampleLocations;
    }

    public static Cell[][] getComboTeams() throws IOException {
        int minSize = 1;
        HashSet<String> comboTeamsAll = new HashSet<>();
        String[][] validComboTeams = new String[32][32];
        Cell[][] grid = new Cell[32][32];
        for(int i=0; i<32; i++){
            for(int j=0; j<32; j++){
                validComboTeams[i][j] = "";
                grid[i][j] = new Cell();
            }
        }


        //ArrayList<String> teams = getTeams();

        for(int i=0; i<teams.size(); i++){
            String teamOne = teams.get(i);
            int numValidForTeam = 0;
            for(int j=0; j<teams.size(); j++){
                HashSet<String> validNamesSet = new HashSet<>();//jt
                if(j==i) {
                    //continue;
                }

                String teamTwo = teams.get(j);
                if(i ==1 && j ==0){
                    int g=1;
                }
                for(int t1=minSize; t1<=teamOne.length(); t1++){
                    String teamOnePrefix = teamOne.substring(0, t1);
                    for(int t2=0; t2<=teamTwo.length()-minSize; t2++){
                        String teamTwoSuffix = teamTwo.substring(t2);
                        String comboTeamA = teamOnePrefix.toLowerCase(Locale.ROOT) + teamTwoSuffix.toLowerCase(Locale.ROOT);
                        boolean isValidWord = wordChecker(comboTeamA);

                        if(isValidWord){
                            String currentTeams = validComboTeams[i][j];
                            String stringToAdd = comboTeamA + " & ";
                            if(!currentTeams.contains(stringToAdd)) {
                                comboTeamsAll.add(comboTeamA);
                                validComboTeams[i][j] += stringToAdd;
                                validNamesSet.add(comboTeamA);
                            }
                        }
                    }
                }
                for(int t1=0; t1<=teamOne.length()-minSize; t1++){
                    String teamOneSuffix = teamOne.substring(t1);
                    for(int t2=minSize; t2<=teamTwo.length(); t2++){
                        String teamTwoPrefix = teamTwo.substring(0,t2);
                        String comboTeamB = teamTwoPrefix.toLowerCase(Locale.ROOT) + teamOneSuffix.toLowerCase(Locale.ROOT);
                        boolean isValidWord = wordChecker(comboTeamB);
                        if(isValidWord){
                            String currentTeams = validComboTeams[i][j];
                            String stringToAdd = comboTeamB + " & ";
                            if(!currentTeams.contains(stringToAdd)){
                                comboTeamsAll.add(comboTeamB);
                                validComboTeams[i][j] += stringToAdd;
                                validNamesSet.add(comboTeamB);
                            }
                        }
                    }
                }
                grid[i][j].setCell(validNamesSet);
            }
        }
        FileWriter writer = new FileWriter("teamComboNames2D.txt");
        for(int i=0; i<32; i++){
            for(int j=0; j<32; j++){
                writer.write( validComboTeams[i][j] + System.lineSeparator());
                //System.out.println(teams.get(i) + "\t" + teams.get(j) + "\t" + validComboTeams[i][j]);
            }
        }
        writer.close();
        return grid;
    }

    public static boolean wordChecker(String comboTeam){

        String comboTeamSingular = comboTeam.substring(0, comboTeam.length()-1);
        boolean i1 = englishWords.contains(comboTeam);
        boolean i2 = !englishNouns.contains(comboTeamSingular);
        boolean isEnglishNoun = englishWords.contains(comboTeam) && englishNouns.contains(comboTeamSingular);
        boolean isBadWord = badWords.contains(comboTeam) || badWords.contains(comboTeamSingular);
        boolean isntSameName = !teams.contains(comboTeam);
        boolean isntManRemWord = !manRemWords.contains(comboTeam);
        boolean isntSelfMerge = !selfMergeWords.contains(comboTeam);
        boolean manAppWord = manAppWords.contains(comboTeam);
        if(manAppWord){
            return true;
        }
        return isEnglishNoun && isntManRemWord && isntSameName;
    }



    public static HashSet<String> getWords(String filepath){
        Scanner s = null;
        try {
            s = new Scanner(new File(filepath));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        HashSet<String> set = new HashSet<String>();
        while (s.hasNext()){
            set.add(s.next());
        }
        s.close();
        return set;
    }





    public static HashSet<String> getEnglishNouns(){
        HashSet<String> nouns = new HashSet<>();
        List<String> lines = Collections.emptyList();
        try {
            lines = Files.readAllLines(Paths.get("data/OEDictionary.txt"), StandardCharsets.UTF_8);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        for(String line : lines){
            if(line.contains(" n.") || line.contains("—n.")){
                nouns.add(line.split(" ")[0].toLowerCase(Locale.ROOT));
            }
        }

        return nouns;

    }

    public static ArrayList<String> getTeams(){
        Scanner s = null;
        try {
            s = new Scanner(new File("data/teamNames.txt"));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        ArrayList<String> list = new ArrayList<String>();
        while (s.hasNext()){
            list.add(s.next());
        }
        s.close();
        return list;
    }

    public static ArrayList<String> getThemes(){
        Scanner s = null;
        try {
            s = new Scanner(new File("data/themes.txt"));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        ArrayList<String> list = new ArrayList<String>();
        while (s.hasNextLine()){
            String line = s.nextLine();
            list.add(line.split(":")[0]);
        }
        s.close();
        return list;
    }

    public static HashMap<String, HashSet<String>> getThemesToWords(){
        Scanner s = null;
        try {
            s = new Scanner(new File("data/themes.txt"));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        HashMap<String, HashSet<String>> themesToWords = new HashMap<>();
        while (s.hasNextLine()){
            String line = s.nextLine();
            String key = line.split(":")[0];
            String[] valueWords = line.split(":")[1].split(" ");
            HashSet<String> value = new HashSet<>();
            value.addAll(Arrays.asList(valueWords));
            themesToWords.put(key, value);
        }
        s.close();
        return themesToWords;
    }

}
