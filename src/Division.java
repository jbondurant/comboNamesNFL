import java.util.ArrayList;

public class Division {
    String name;
    ArrayList<Team> teams;

    public Division(String n){
        name = n;
        teams = new ArrayList<Team>();
    }

    public Division(String n, ArrayList<Team> t){
        name = n;
        teams = t;
    }

    public void PrintDivision(){
        System.out.println("\t----- " + name + " Division -----");
        for(Team t : teams){
            t.PrintTeam();
        }
    }
}
