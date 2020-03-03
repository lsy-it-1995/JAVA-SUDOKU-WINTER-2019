import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.TreeMap;

public class BTSolver
{

	// =================================================================
	// Properties
	// =================================================================

	private ConstraintNetwork network;
	private SudokuBoard sudokuGrid;
	private Trail trail;

	private boolean hasSolution = false;

	public String varHeuristics;
	public String valHeuristics;
	public String cChecks;

	// =================================================================
	// Constructors
	// =================================================================

	public BTSolver ( SudokuBoard sboard, Trail trail, String val_sh, String var_sh, String cc )
	{
		this.network    = new ConstraintNetwork( sboard );
		this.sudokuGrid = sboard;
		this.trail      = trail;

		varHeuristics = var_sh;
		valHeuristics = val_sh;
		cChecks       = cc;
	}

	// =================================================================
	// Consistency Checks
	// =================================================================

	// Basic consistency check, no propagation done
	private boolean assignmentsCheck ( )
	{
		for ( Constraint c : network.getConstraints() )
			if ( ! c.isConsistent() )
				return false;

		return true;
	}

	/**
	 * Part 1 TODO: Implement the Forward Checking Heuristic
	 *
	 * This function will do both Constraint Propagation and check
	 * the consistency of the network
	 *
	 * (1) If a variable is assigned then eliminate that value from
	 *     the square's neighbors.
	 *
	 * Note: remember to trail.push variables before you change their domain
	 * Return: true is assignment is consistent, false otherwise
	 */
	private boolean forwardChecking ( )
	{
		for ( Variable v : network.getVariables() ){
			if(v.isAssigned()){
				List<Variable> neighbors = network.getNeighborsOfVariable(v);
				for(Variable temp: neighbors){
					if(temp.getValues().contains(v.getAssignment())){
						if(temp.size() == 1) return false;
						trail.push(temp);
						temp.removeValueFromDomain(v.getAssignment());
					}
				}
			}
		}

		return assignmentsCheck();	
	}
        /**
	 * Part 2 TODO: Implement both of Norvig's Heuristics
	 *
	 * This function will do both Constraint Propagation and check
	 * the consistency of the network
	 *
	 * (1) If a variable is assigned then eliminate that value from
	 *     the square's neighbors.
	 *
	 * (2) If a constraint has only one possible place for a value
	 *     then put the value there.
	 *
	 * Note: remember to trail.push variables before you change their domain
	 * Return: true is assignment is consistent, false otherwise
	 */

	private boolean norvigCheck ( )
	{
		if(!forwardChecking()) return false;

		int boardSize = sudokuGrid.getN();
		List<Variable> rows = new ArrayList<>(boardSize);
		List<Variable> cols = new ArrayList<>(boardSize);
		List<Variable> block = new ArrayList<>(boardSize);

		for(int i = 0; i < boardSize; i++){
			// Sets all variables in the current row/col (works diagonally)

			setRowColBlock(rows,cols,block,i);

			norvigHelper(rows);
			norvigHelper(cols);
			norvigHelper(block);
		}



		return assignmentsCheck();
	}

	void norvigHelper(List<Variable> list){
		for(Variable v: list){

			if(v.size() == 1) continue; // i just put this

			// save the domain of current variable
			List<Integer> domain = new ArrayList<>(v.getValues());

			// second loop for checking variables against other variables in the same row
			for(Variable temp: list){
				if(!v.equals(temp)){
					// remove all values from domain that appear in other variable's domain
					List<Integer> tempDomain = temp.getDomain().getValues();
					for(Integer value: tempDomain){
						domain.remove(value);
					}
				}
			}

			if(domain.size() == 1){
				trail.push(v);
				v.assignValue(domain.get(0));
			}
		}
	}


	private void setRowColBlock(List<Variable> row, List<Variable> col, List<Variable> block, int index) {
		row.clear();
		col.clear();
		block.clear();
		for (Variable temp : network.getVariables()) {
			if (temp.row() == index) row.add(temp);
			if (temp.col() == index) col.add(temp);
			if (temp.block() == index) block.add(temp);
		}

	}

	/**
	 * Optional TODO: Implement your own advanced Constraint Propagation
	 *
	 * Completing the three tourn heuristic will automatically enter
	 * your program into a tournament.
	 */
	private boolean getTournCC ( )
	{
                if(!arcConsistency()) return false;

		int boardSize = sudokuGrid.getN();
		List<Variable> rows = new ArrayList<>(boardSize);
		List<Variable> cols = new ArrayList<>(boardSize);
		List<Variable> block = new ArrayList<>(boardSize);

		for(int i = 0; i < boardSize; i++){
			// Sets all variables in the current row/col (works diagonally)

			setRowColBlock(rows,cols,block,i);

			norvigHelper(rows);
			norvigHelper(cols);
			norvigHelper(block);
		}

		return assignmentsCheck();

	}

        private boolean arcConsistency(){
		boolean foundOne;

		do{
			foundOne = false;
			for ( Variable v : network.getVariables() ){
				if(v.isAssigned()){
					List<Variable> neighbors = network.getNeighborsOfVariable(v);
					for(Variable temp: neighbors){
						if(temp.getValues().contains(v.getAssignment())){
							if(temp.size() == 1){
								return false;
							}
							foundOne = true;
							trail.push(temp);
							temp.removeValueFromDomain(v.getAssignment());
							if(temp.size() == 1) temp.assignValue(temp.getValues().get(0));
						}
					}
				}
			}
		}while(foundOne == true);

		return assignmentsCheck();
	}


	// Variable Selectors

	// =================================================================

	// Basic variable selector, returns first unassigned variable
	private Variable getfirstUnassignedVariable()
	{
		for ( Variable v : network.getVariables() )
			if ( ! v.isAssigned() )
				return v;

		// Everything is assigned
		return null;
	}

	/**
	 * Part 1 TODO: Implement the Minimum Remaining Value Heuristic
	 *
	 * Return: The unassigned variable with the smallest domain
	 */
	private Variable getMRV ( )
	{
		Variable smallestDomain = null;
		for ( Variable v : network.getVariables() ){
			if(!v.isAssigned() && (smallestDomain == null || v.size() < smallestDomain.size())){
				smallestDomain = v;
			}
		}

		return smallestDomain;
	}

	/**
	 * Part 2 TODO: Implement the Minimum Remaining Value Heuristic
	 *                with Degree Heuristic as a Tie Breaker
	 *
	 * Return: The unassigned variable with, first, the smallest domain
	 *         and, second, the most unassigned neighbors
	 */
	

	private Variable MRVwithTieBreaker ( )
	{


		Variable smallestDomain = getMRV();
		int smallestDegree = -1;

		if(smallestDomain==null) return null;
		int degreeCounter;
		List<Variable> neighbors;
		// Add all of the domains with equal size to the smallest domain to the smallestDomains list
		for ( Variable v : network.getVariables() ){
			if(!v.isAssigned() && (smallestDomain.size() == v.size())) {
				degreeCounter = 0;

				neighbors = network.getNeighborsOfVariable(v);//getNeighbors(v);
				for(Variable n: neighbors){
					if(n.isAssigned()){
						degreeCounter++;
					}
				}
				if(degreeCounter < smallestDegree || smallestDegree == -1){
					smallestDegree = degreeCounter;
					smallestDomain = v;
				}
			}
		}
		return smallestDomain;
	}	

	/**
	 * Optional TODO: Implement your own advanced Variable Heuristic
	 *
	 * Completing the three tourn heuristic will automatically enter
	 * your program into a tournament.
	 */
	private Variable getTournVar ( )
	{
		return MRVwithTieBreaker();
	}

	// =================================================================
	// Value Selectors
	// =================================================================

	// Default Value Ordering
	public List<Integer> getValuesInOrder ( Variable v )
	{
		List<Integer> values = v.getDomain().getValues();

		Comparator<Integer> valueComparator = new Comparator<Integer>(){

			@Override
			public int compare(Integer i1, Integer i2) {
				return i1.compareTo(i2);
			}
		};
		Collections.sort(values, valueComparator);
		return values;
	}

	/**
	 * Part 1 TODO: Implement the Least Constraining Value Heuristic
	 *
	 * The Least constraining value is the one that will knock the least
	 * values out of it's neighbors domain.
	 *
	 * Return: A list of v's domain sorted by the LCV heuristic
	 *         The LCV is first and the MCV is last
	 */
	public List<Integer> getValuesLCVOrder ( Variable v )
	{
	
		List<Variable> neighbors = network.getNeighborsOfVariable(v); //getNeighbors(v);
		List<Integer> lcv = new ArrayList<>();
		Map<Integer,Integer> map = new TreeMap<>();

		int value;
		for(int i = 0; i < v.size(); i++){
			value  = 0;
			for(Variable temp: neighbors){
				if(temp.getDomain().contains(v.getDomain().getValues().get(i))){
					value++;
				}
			}
			map.put(i,value);
		}

		for(int i = 0; i < map.size(); i++){
			Integer smallestValueIndex = null;
			for(int j = 0; j < map.size(); j++){
				if(map.get(j) != -1 && (smallestValueIndex == null || map.get(j) < map.get(smallestValueIndex)) ){
					smallestValueIndex = j;
				}
			}
			lcv.add(v.getDomain().getValues().get(smallestValueIndex));
			map.put(smallestValueIndex, -1);
		}

		return lcv;

	}

	/**
	 * Optional TODO: Implement your own advanced Value Heuristic
	 *
	 * Completing the three tourn heuristic will automatically enter
	 * your program into a tournament.
	 */
	public List<Integer> getTournVal ( Variable v )
	{
		return getValuesLCVOrder(v);
	}

	//==================================================================
	// Engine Functions
	//==================================================================

	public void solve ( )
	{
		if ( hasSolution )
			return;

		// Variable Selection
		Variable v = selectNextVariable();

		if ( v == null )
		{
			for ( Variable var : network.getVariables() )
			{
				// If all variables haven't been assigned
				if ( ! var.isAssigned() )
				{
					System.out.println( "Error" );
					return;
				}
			}

			// Success
			hasSolution = true;
			return;
		}

		// Attempt to assign a value
		for ( Integer i : getNextValues( v ) )
		{
			// Store place in trail and push variable's state on trail
			trail.placeTrailMarker();
			trail.push( v );

			// Assign the value
			v.assignValue( i );

			// Propagate constraints, check consistency, recurse
			if ( checkConsistency() )
				solve();

			// If this assignment succeeded, return
			if ( hasSolution )
				return;

			// Otherwise backtrack
			trail.undo();
		}
	}

	public boolean checkConsistency ( )
	{
		switch ( cChecks )
		{
			case "forwardChecking":
				return forwardChecking();

			case "norvigCheck":
				return norvigCheck();

			case "tournCC":
				return getTournCC();

			default:
				return assignmentsCheck();
		}
	}

	private Variable selectNextVariable ( )
	{
		switch ( varHeuristics )
		{
			case "MinimumRemainingValue":
				return getMRV();

			case "MRVwithTieBreaker":
				return MRVwithTieBreaker();

			case "tournVar":
				return getTournVar();

			default:
				return getfirstUnassignedVariable();
		}
	}

	public List<Integer> getNextValues ( Variable v )
	{
		switch ( valHeuristics )
		{
			case "LeastConstrainingValue":
				return getValuesLCVOrder( v );

			case "tournVal":
				return getTournVal( v );

			default:
				return getValuesInOrder( v );
		}
	}

	public boolean hasSolution ( )
	{
		return hasSolution;
	}

	public SudokuBoard getSolution ( )
	{
		return network.toSudokuBoard ( sudokuGrid.getP(), sudokuGrid.getQ() );
	}

	public ConstraintNetwork getNetwork ( )
	{
		return network;
	}
}
