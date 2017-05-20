package sudoku_csp;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.*;

/**
 * This class corresponds to an implementation of a 
 * Sudoku solver considering the Sudoku a CSP or 
 * Constraint Satisfaction Problem.
 */
public class sudoku_csp {
	
	/**
	 * Size of the Sudoku board, commonly is 9
	 */
	public Integer boardSize;
	/**
	 * List of cells or tiles Sudoku board
	 */ 
	public List<Cell> cells;
	/**
	 * List of all constraints used to model the problem
	 */ 
	public List<AllDiffConstraint> rules;
	/**
	 * Initial state of the board
	 */ 
	public State base;
	/**
	 * Map that relate a given cell and its constraints
	 */ 
	public Map<Cell, List<AllDiffConstraint>> cellRules;
	/**
	 * Variable used to count the number of nodes expanded by the algorithm.
	 * For purposes of performance analysis
	 */
	private int nodes;
	
	/**
	 * Variable used to measure the execution time of the algorithm.
	 * For purposes of performance analysis
	 */
	private double time;

	/**
	 * Constructor of the Sudoku board.
	 * @param size: Size of the board.
	 */ 
	public sudoku_csp(int size) {
		// Generate everything
		base = new State();
		boardSize = size;
		rules = new ArrayList<AllDiffConstraint>(boardSize*3);
		cells = new ArrayList<Cell>(boardSize*boardSize);
		cellRules = null;
		nodes=0;
		time=0;
		for (int i=0;i<boardSize*boardSize;i++) {
			cells.add(new Cell(i,null));
		}
		init_constraints();
	}
	
	/**
	 * It verifies if the Sudoku is solved with a given state.
	 * @param state: A given state of the board.
	 */ 
	public boolean sudoku_solved(State state) {
		if (cells.size() > state.assignments.size()){
			return false;
		}
		for (AllDiffConstraint adc : rules) {
			if (!adc.satisfied(state)) {
				return false;
			}
		}
		return true;
	}
	
	/**
	 * It selects all the blank or unassigned cells in a given state.
	 * @param state: A given state of the board.
	 */ 
	public List<Cell> all_blank_cells(State state) {
		List<Cell> list = new LinkedList<Cell>();
		for (Cell c : cells) {
			if (state.assignments.get(c) == null)
				list.add(c);
		}
		return list;
	}
	
	/**
	 * It selects a blank or unassigned cell in a given state.
	 * It returns the first that can find.
	 * @param state: A given state of the board.
	 */ 
	public Cell blank_cell(State state) {
		for (Cell c : cells) {
			if (state.assignments.get(c) == null)
				return c;
		}
		return null;
	}

	/**
	 * It selects a blank or unassigned cell.
	 * Use of the heuristic Most Constrained Variable (MCedV)
	 * to find the variable with smaller domain
	 * @param state: A given state of the board.
	 */ 
	public Cell blank_cell_heuristic_MCedV(State state) {
		int min = Integer.MAX_VALUE;
		Cell minCell = null;
		
		if (cells.size() == state.assignments.size())
			return null;

		for (Cell c : cells) {
			if (state.assignments.get(c) == null) {
				int numValues = values_cell(state, c).size();
				if (numValues < min) {
					min = numValues;
					minCell = c;
				}
			}
		}

		return minCell;
	}
	
	/**
	 * It selects a blank or unassigned cell.
	 * Use of the heuristic Most Constrained Variable (MCedV)
	 * to find the variable with smaller domain.
	 * Use of the heuristic Most Constraining Variable (MCingV)
	 * as a tiebraker when multiple cells are found using MCedV
	 * @param state: A given state of the board.
	 */ 
	public Cell blank_cell_heuristic_MCedV_MCingV(State state) {
		List<Cell> l1;
		if (cells.size() == state.assignments.size())
			return null;
		//Use of a TreeMap to store a list of the cells 
		//sorted by the size of their domains
		
		//First MCedV is used
		TreeMap<Integer, List<Cell>> tree = new TreeMap<Integer, List<Cell>>();
		for (Cell c : cells) {
			if (state.assignments.get(c) == null) {
				int numValues = values_cell(state, c).size();
				if(tree.containsKey(numValues)){
					tree.get(numValues).add(c);
				}else{
					l1 = new LinkedList<Cell>();
					l1.add(c);
					tree.put(numValues, l1);
				}
			}
		}
		
		//Second MCingV is applied
		l1= tree.get(tree.firstKey());
		int max = Integer.MIN_VALUE;
		int rul=0;
		Cell bestCell = null;
		if(l1.size()==1)
			return l1.get(0);
		else{
			for (int i = 0; i < l1.size(); i++) {
				for(AllDiffConstraint adf : rules){
					if(adf.cells.contains(l1.get(i)))
						rul++;
				}
				if (rul > max) {
					max = rul;
					bestCell = l1.get(i);
				}
			}
		}
		return bestCell;
	}
	
	/**
	 * Use of the heuristic Least Constraining Value (LCV)
	 * to find the best value to being assigned for a blank cell.
	 * @param state: A given state of the board.
	 * @param domain: The domain of the cell.
	 * @param cell: The blank cell that will be assigned.
	 */ 
	public Object best_value_heuristic_LCV(State state, List<Object> domain, Cell cell) {
		List<Cell> blanks = all_blank_cells(state);
		int count, min = Integer.MAX_VALUE;
		Object best=null;
		for(Object v: domain){
			count=0;
			for (Cell c: blanks){
				if(c.id!= cell.id){
					if(state.domains.get(c).contains(v) && search_rule(c, cell))
						count++;
				}
			}
			if(count<min){
				min=count;
				best= v;
			}
		}
		return best;
	}
	
	/**
	 * Forward Checking Algorithm.
	 * It evaluates how an assignment over a cell affects the rest of the cells.
	 * @param state: A given state of the board.
	 * @param cell: The cell that was assigned.
	 */ 
	public State forward_checking(State state, Cell cell) {
		List<AllDiffConstraint> rul = rules_cell(cell);
		Object value = state.assignments.get(cell);

		for (AllDiffConstraint adc : rul) {
			for (Cell c : adc.cells) {
				if (c == cell)
					continue;

				List<Object> values = values_cell(state, c);
				if (values.contains(value)) {
					values = new LinkedList<Object>(values);
					values.remove(value);
					state.domains.put(c, values);
					if (state.assignments.get(c) == null) {
						if (values.size() == 1) {
							state = state.assign(c, values.get(0));
						} else if (values.size() == 0) {
							continue;
						}
					}
				}
			}
		}

		return state;
	}
	
	/**
	 * Returns the rules that apply/affect a given cell.
	 * @param cell: A given cell.
	 */ 
	public List<AllDiffConstraint> rules_cell(Cell cell) {
		if (cellRules != null)
			return cellRules.get(cell);
		cellRules = new HashMap<Cell, List<AllDiffConstraint>>();

		for (AllDiffConstraint adc : rules) {
			for (Cell c : adc.cells) {
				if (cellRules.containsKey(c)) {
					cellRules.get(c).add(adc);
				} else {
					List<AllDiffConstraint> rul = new LinkedList<AllDiffConstraint>();
					rul.add(adc);
					cellRules.put(c, rul);
				}
			}
		}
		return cellRules.get(cell);
	}


	/**
	 * Returns the values in the domain of a given cell in a given state.
	 * @param state: A given state of the board.
	 * @param cell: A given cell.
	 */ 
	public List<Object> values_cell(State state, Cell cell) {
		List<Object> values = state.domains.get(cell);
		if (values != null)
			return values;
		return cell.domain;
	}

	/**
	 * Verifies the consistency of a new state.
	 * @param state: A given state of the board.
	 */ 
	public boolean consistent_state(State state) {
		for (AllDiffConstraint adc : rules) {
			if (!adc.consistent(state))
				return false;
		}
		return true;
	}
	
	/**
	 * Backtracking Search Algorithm.
	 * It calls the recursive backtracking with the base state.
	 * @param type: Type of the combination of algorithms and heuristics that will be used.
	 * type = 0, backtracking search
	 * type = 1, backtracking search with forward checking
	 * type = 2,  backtracking search with forward checking and 3 heuristics (MCedV, MCingV, LCV)
	 */  
	public State backtracking_search(int type){
		nodes=0;
		long t1 = System.nanoTime();
		State s1 = base;
		State s2 = recursive_backtrack(s1, type);
		long t2 = System.nanoTime();
		time = (double)(t2-t1) / 1000000000.0;
		return s2;
	}

	/**
	 * Backtracking Recursive Algorithm.
	 * It selects add blank cell (with unassigned value), 
	 * assign one of the values in the domain of the cell, 
	 * executes forward checking, and repeats this process 
	 * until there is no remaining candidate value or a 
	 * solution has been found.
	 * @param type: Type of the combination of algorithms and heuristics that will be used.
	 * type = 0, backtracking search
	 * type = 1, backtracking search with forward checking
	 * type = 2,  backtracking search with forward checking and 3 heuristics (MCedV, MCingV, LCV)
	 */  
	public State recursive_backtrack(State state, int type){
		nodes++;
		if (sudoku_solved(state)) {
			return state;
		}

		Cell cell;
		if(type==2){ //Use of heuristics MCedV and MCingV to select the cell
			//c = blank_cell_heuristic_MCedV(a);
			cell = blank_cell_heuristic_MCedV_MCingV(state);
		}
		else //No heuristic to select the cell
			cell= blank_cell(state);
		if (cell == null)
			return null;
		
		List<Object> values = values_cell(state, cell);
		if(type==2){ //Use of heuristic LCV to select the value for the cell
			Object value = best_value_heuristic_LCV(state, values, cell);
			values.remove(value);
			values.add(0, value);
		}
		for (Object value : values) {
			State a2 = state.assign(cell, value);

			if(type>0)//Perform forward checking for types 1 and 2
				a2 = forward_checking(a2, cell);
			if (!consistent_state(a2)) 
				continue; 
			a2 = recursive_backtrack(a2, type);
			if (a2 != null)
					return a2;
		}
		return null;
	}
	
	/**
	 * It initializes the rules for the Sudoku board.
	 */  
	public void init_constraints() {
		if (rules.size() == 0) {
			
			// ROW constraints
			for (int row=0;row<boardSize;row++) {
				AllDiffConstraint rule = new AllDiffConstraint(); 
				for (int col=0;col<boardSize;col++) {
					rule.cells.add(cells.get(row*boardSize+col));
				}
				rules.add(rule);
			}

			// COLUMN constraints
			for (int col=0;col<boardSize;col++) {
				AllDiffConstraint rule = new AllDiffConstraint(); 
				for (int row=0;row<boardSize;row++) {
					rule.cells.add(cells.get(row*boardSize+col));
				}
				rules.add(rule);
			}

			// SUB-SQUARES constraints
			int gridSize = (int) Math.sqrt(boardSize);
			for(int row=0;row<gridSize;row++){
				for(int col=0;col<gridSize;col++){
					AllDiffConstraint rule = new AllDiffConstraint(); 
					for(int rowSS=0;rowSS<gridSize;rowSS++){
						for(int colSS=0;colSS<gridSize;colSS++){
							rule.cells.add(rules.get(rowSS+row*gridSize).cells.get(colSS+col*gridSize));
						}
					}
					rules.add(rule);
				}
			}			
			
			// There has to be a more efficient way that 4 consecutive loops, right?
//			for(int rowSS=0,colSS=0;rowSS<gridSize && colSS<gridSize;colSS++){
//				AllDiffConstraint rule = new AllDiffConstraint();
//				start = (rowSS * gridSize * boardSize) + (colSS * gridSize);
//				for (int x = 0,y = 0; y< gridSize; x++) {
//					move = start + x + y*boardSize;
//					rule.cells.add(cells.get(move));
//					if(x==gridSize-1){
//						x=0;
//						y++;
//					}
//				}
//				if(colSS==gridSize-1){
//					colSS=0;
//					rowSS++;
//				}
//				rules.add(rule);
//			}
		}
	}
	
	/**
	 * Verifies it exits at least one constraint that relates between two given cells.
	 * @param c1: A given cell.
	 * @param c2: A given cell.
	 */ 
	public boolean search_rule(Cell c1, Cell c2){
		for(AllDiffConstraint adc: rules){
			if(adc.contains_cell(c1.id) && adc.contains_cell(c2.id))
				return true;
		}
		return false;
	}

	/**
	 * This class represents a cell or tile in the Sudoku board.
	 */
	public class Cell{
		/**
		 * It corresponds to the position in the board.
		 */
		public int id;
		/**
		 * Possible values of the cell.
		 */
		public List<Object> domain;
		/**
		 * Current value of the cell.
		 */
		public Object value;

		public Cell(int id, Object value) {
			this.id = id;
			domain = new LinkedList<Object>();
			if(value == null){
				for (int x = 1; x < 10; x++) {
					domain.add(x);
				}
			}
		}
		
		@Override
		public String toString() {
			return (id)+"-"+value;//+"-"+domain.toString();
		}
	}

	/**
	 * This class represents a allDiff Constraint.
	 */
	public class AllDiffConstraint {
		/**
		 * List of cells related to the allDiff Constraint.
		 */
		public List<Cell> cells;
		
		public AllDiffConstraint(){
			cells = new LinkedList<Cell>();
		}
		
		@Override
		public String toString() {
			String msg="";
			for(Cell c: cells)
				msg += c.toString()+"\n";
			return msg;
		}

		public boolean satisfied(State state) {
			boolean[] visited = new boolean[boardSize+1];
			for (Cell cell : cells) {
				Integer value = (Integer) state.assignments.get(cell);
				if (value == null || visited[value])
					return false;
				visited[value] = true;
			}
			return true;
		}

		public boolean consistent(State state) {
			boolean[] visited = new boolean[boardSize+1];
			boolean[] free = new boolean[boardSize+1];
			int numValues = 0;

			for (Cell cell : cells) {
				for (Object value : values_cell(state,cell)) {
					if (!free[(Integer)value]) {
						numValues++;
						free[(Integer)value] = true;
					}
				}

				Integer value = (Integer)state.assignments.get(cell);
				if (value != null) {
					if (visited[value])
						return false;
					visited[value] = true;
				}
			}

			if (cells.size() > numValues)
				return false;
			return true;
		}
		
		public boolean contains_cell(int id){
			for (Cell cell : cells) {
				if(cell.id==id)
					return true;
			}
			return false;
		}
	}

	/**
	 * This class represents a state of the Sudoku board.
	 */
	public class State {
		/**
		 * Current cell assignments.
		 */
		public Map<Cell, Object> assignments = null;

		/**
		 * Currant domains of cells
		 */
		public Map<Cell, List<Object>> domains = null;

		/**
		 * Creates a new blank assignment.
		 */
		public State() {
			assignments = new HashMap<Cell, Object>();
			domains = new HashMap<Cell, List<Object>>();
		}

		/**
		 * Assigns a value to a cell and returns the new state
		 */
		public State assign(Cell cell, Object value) {
			State s2 = new State();
			s2.assignments = new HashMap<Cell, Object>(assignments);
			s2.assignments.put(cell, value);
			s2.domains = new HashMap<Cell, List<Object>>(domains);

			// Restrict the domain to only a single value
			List<Object> varDomain = new LinkedList<Object>();
			varDomain.add(value);
			s2.domains.put(cell, varDomain);
			
			return s2;
		}
		
		@Override
		public String toString() {
			String s= "";
			for(Cell c : assignments.keySet())
				s+= c.toString()+"|="+assignments.get(c)+"\n";
			s+="\n";
			for(Cell c : domains.keySet())
				s+= c.toString()+"|="+domains.get(c).toString()+"\n";
			return s;
		}
		
		public String getSudokuBoard(){
			String board="";
			for (int x = 0; x < boardSize; x++) {
				for (int y = 0; y < boardSize; y++) {
					board+=assignments.get(cells.get(x*boardSize+y))+"|";
				}
				board+="\n";
			}
			return board;
		}
	}

	public static void main(String[] args) {
		sudoku_csp sudo, old_sudo = null;
		int runs=1;
		try {
			for (int type = 0; type < 3; type++) {
				BufferedReader in = new BufferedReader(new FileReader("./data/test.txt"));
				String line;
				int board=0;
				while( (line = in.readLine()) != null){
					for (int run = 0; run < runs; run++) {
						if(run==0){
							int size = new Integer(line);
							sudo = new sudoku_csp(size);
							int cell_id, cell_num;
							String row;
							for (int x = 0; x < sudo.boardSize; x++) {
								row=in.readLine();
								for (int y = 0; y < sudo.boardSize; y++) {
									cell_id = x * 9 + y;
									if(row.charAt(y) != 'x'){
										cell_num = Character.getNumericValue(row.charAt(y));
										Cell cell = sudo.cells.get(cell_id);
										cell.value=cell_num;
										cell.domain= new LinkedList<Object>();
										sudo.cells.set(cell_id,cell);

										sudo.base = sudo.base.assign(cell, cell_num);
										if(type>0)
											sudo.base = sudo.forward_checking(sudo.base, cell);
									}
								}
							}
							old_sudo = sudo;
						}else
							sudo = old_sudo;
						
						State solution = sudo.backtracking_search(type);
						if (solution == null){
							System.out.println("BAD\tb"+board+"\tt"+type+"\tr"+(run+1)+"\tnodes "+sudo.nodes+"\ttime "+sudo.time);
							continue;
						}
						System.out.println("OK\tb"+board+"\tt"+type+"\tr"+(run+1)+"\tnodes "+sudo.nodes+"\ttime "+sudo.time);
						//System.out.println(solution.getSudokuBoard());
					}
					board++;
					//System.out.println();
				}
				in.close();
			}
		}catch (Exception e) {
			//e.printStackTrace();
			System.out.println("Error");
			//System.exit(1);
		}
	}
}
