public class Team {
    String comboName;
    String teamA;
    String teamB;

    public Team(String cn, String ta, String tb){
        comboName = cn;
        teamA = ta;
        teamB= tb;
    }

    public void PrintTeam(){
        System.out.println("\t\t----- " + comboName);
        System.out.println("\t\t\t----- " + teamA);
        System.out.println("\t\t\t----- " + teamB);
    }
}
