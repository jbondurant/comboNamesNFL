import java.util.ArrayList;

public class League {
    public ArrayList<Division> divisions;


    public League(ArrayList<Division> d){
        divisions = d;
    }

    public void PrintLeague(){
        System.out.println("----- League -----");
        for(Division d : divisions){
            d.PrintDivision();

        }
    }
}
