/**
 * BisimulationChecker.java
 * Author: Joshua Parker
 */
import java.io.*;
import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Arrays;
import java.util.Comparator;

public class BisimulationChecker {
    Process myP, myQ;
    Set<Set<String>> finalOut;
    
    /**
     * BisumlationChecker constructor
     */
    public BisimulationChecker() {
        this.myP = null;
        this.myQ = null;
        this.finalOut = null;
    }

    /**
     * Gets the user to enter a filename to load loops until valid filename given
     * @param filename - the name of the file to load
     * @param fileNumber - a string containing the number of the file to awaiting user input
     * @return filename - a String containing a valid filename
     */
    private String readInputFile(String filename, String processLetter) throws IOException {
    	while (filename == null || !new File(filename).exists()) {
            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
            System.out.print("Enter a string for " + processLetter + ": ");
            System.out.flush();
		    filename = br.readLine();
        }
    	return filename;
    }
    
    /**
     * Reads the given input files to make the LTS
     * @param fileP - filename of the P file
     * @param fileQ - filename of the Q file
     */
    public void readInput(String fileP, String fileQ) {
    	String p = "P", q = "Q";
    	try {	
	    	BufferedReader brP = new BufferedReader(new FileReader(readInputFile(fileP, p)));
	        myP = makeLTS(brP, p);
	        brP.close();
	
	        BufferedReader brQ = new BufferedReader(new FileReader(readInputFile(fileQ, q)));
	        myQ = makeLTS(brQ, q);
	        brQ.close();
		}catch (NullPointerException e){
			readInput(null, null);
		}catch (ArrayIndexOutOfBoundsException e) {
			readInput(null, null);
    	}catch(Exception e){
    		e.printStackTrace();
    	}
    }

    /**
     * Makes each process from the files given
     * @param br - bufferedreader to read in file
     * @param processName - current process we are loading in
     * @return - null on fail
     */
	private Process makeLTS(BufferedReader br, String processName) throws IOException {
        Process pr = new Process();
        String line = "";
        while (line != null) {
            line = br.readLine();
            if (line.startsWith("!"))
                return pr;

            String values[] = line.split("[,:]"), 					// values:
                   source = processName + values[0].trim(), 		//   start
                   action = values[1].trim(),           			//  action
                   destination = processName + values[2].trim();   	//     end

            pr.states.add(source);
            pr.actions.add(action);
            pr.states.add(destination);
            pr.transitions.add(new Process.Transition(source, destination, action));
        }
        return null;
    }

	/**
     * Performs bisimulation on desired processes
     */
    public void performBisimulation() {
    	if (myP == null || myQ == null)
    		System.err.println("No valid input files given");
    	else
    		finalOut = bisimulationComputation(myP, myQ);
    }
	
    /**
     * Performs the bisimulation computation based on the algorithm from the course book
     * @param p	- Process P
     * @param q	- Process Q
     * @return	rho - the final output of the bisimulation check
     */
    private Set<Set<String>> bisimulationComputation(Process p, Process q) {
    	// 1.
		Set<Process.Transition> ts = new HashSet<Process.Transition>();
    	ts.addAll(p.transitions);
    	ts.addAll(q.transitions);

		Set<String> sigma = new HashSet<String>();
    	sigma.addAll(p.actions);
    	sigma.addAll(q.actions);

    	Set<Set<String>> rho1 = new HashSet<Set<String>>();
    	Set<String> initial = new HashSet<String>();
    	rho1.add(initial);
    	initial.addAll(p.states);
    	initial.addAll(q.states);
		
    	// 2.
    	Set<Set<String>> rho = new HashSet<Set<String>>(rho1);

    	// 3.
    	Set<Set<String>> waiting = new HashSet<Set<String>>(rho1);

    	// 4.
    	while (!waiting.isEmpty()) {
    		// 4.1
			Set<String> pPrime = waiting.iterator().next();
			waiting.remove(pPrime);

    		// 4.2
    		for (String a : sigma) {
    			// 4.2.1
				Set<Set<String>> matchP = new HashSet<Set<String>>();
				
				for (Set<String> s : rho) {
					Set<String> taP = buildTaP(s, pPrime, a, ts);

					if (!taP.isEmpty() && !taP.equals(s))
						matchP.add(s);
				}

				// 4.2.2
    			for (Set<String> p2 : matchP) {
    				Set<Set<String>> splitP = split(p2, a, pPrime, ts);

    				rho.remove(p2);
    				rho.addAll(splitP);

    				waiting.removeAll(p2);
    				waiting.addAll(splitP);
    			}
    		}
    	}

    	return rho;
	}
    
    /**
     * based on Ta[P] from course book
     */
    private static Set<String> buildTaP(Set<String> p, Set<String> pPrime, String a, Set<Process.Transition> ts) {
		Set<String> out = new HashSet<String>();

		for (String s : p)
			for (String sPrime : pPrime)
				if (ts.contains(new Process.Transition(s, sPrime, a))) {
					out.add(s);
					break;
				}

        return out;
    }
    
	/**
	 *  Split based on course book
	 */
    private static Set<Set<String>> split(Set<String> p, String a, Set<String> pPrime, Set<Process.Transition> ts) {
    	Set<Set<String>> splitP = new HashSet<Set<String>>();
        Set<String> tap = buildTaP(p, pPrime, a, ts);
        Set<String> partition = new HashSet<String>(p);

		partition.removeAll(tap);
		splitP.add(tap);
		splitP.add(partition);

        return splitP;
    }

    /**
     * Writes bisimulation results to file
     * @param filename - the filename to store the results
     */
    public void writeOutput(String filename) {
    	try {
	        while (filename == null || filename.length() < 1) {
	            BufferedReader r = new BufferedReader(new InputStreamReader(System.in));
	            System.out.print("Enter an output filename: ");
	            System.out.flush();
	            filename = r.readLine();
	            r.close();
	        }
	
	        BufferedWriter out = new BufferedWriter(new FileWriter(filename));
	        out.write("Process P\n" + myP.finalOutput());
	        out.write("Process Q\n" + myQ.finalOutput());
	        out.write("Bisimulation Results\n");
	        boolean bisimilar = true;
	        for (Set<String> states : this.finalOut) {
	            Set<Character> prefix = new HashSet<Character>();
	
	            for (String s : states)
	                prefix.add(s.charAt(0));
	
	            bisimilar = prefix.contains('P') && prefix.contains('Q') && bisimilar;
	            Iterator<String> it = states.iterator();
	            while (it.hasNext())
	                out.write(it.next().substring(1) + (it.hasNext() ? "," : ""));
	            
	            out.write("\n");
	        }
	        out.write("Bisimulation Answer\n" + (bisimilar ? "Yes" : "No"));
	        out.close();
    	}catch(IOException e){ 
    		e.printStackTrace();
    	}
    }
    
    /**
     * Main Method
     */
    public static void main(String[] args) {
        BisimulationChecker checker = new BisimulationChecker();
        checker.readInput(null, null);
        checker.performBisimulation();
        checker.writeOutput(null);
    }
}

/**
* The process class to create each processes values
*/
class Process {
    HashSet<String> states, actions;
    HashSet<Transition> transitions;

    /** Transition Class to keep track of transitions */
    public static class Transition {
        private String source, destination, action;

        /** Constructor */
        public Transition(String source, String destination, String action) {
            this.source = source;
            this.destination = destination;
            this.action = action;
        }
        
        /**
         * Formats the transition for output
         * @return - the formatted string
         */
        public String transitionOutput() {
            return "(" + source.substring(1) + "," + action + "," + destination.substring(1) + ")";
        }
        
        /** Override the default hashCode and equals functions */
        @Override
        public int hashCode() {
            return Arrays.hashCode(new String[] { source, destination, action });
        }

        @Override
        public boolean equals(Object compare) {
            return this.source.equalsIgnoreCase(((Transition) compare).source)
                    && this.action.equalsIgnoreCase(((Transition) compare).action)
                    && this.destination.equalsIgnoreCase(((Transition) compare).destination);
        }
    }
    
    /** Constructor */
    public Process() {
        this.states = new HashSet<String>();
        this.actions = new HashSet<String>();
        this.transitions = new HashSet<Transition>();
    }

    /**
     * Iterates through a list and appends ',' where needed
     * @param str - string to look at
     * @param sub - boolean if true add ','
     * @return - modified string for output
     */
    private String iterateList(String[] str, boolean sub){
        String out = "";
        Iterator<String> it = Arrays.asList(str).iterator();
        while (it.hasNext()) {
            out += (sub ? it.next().substring(1) : it.next());
            out = (it.hasNext() ? out += "," : out);
        }
        return out;
    }
    
    /**
     * Makes the desired format for the output file
     * @return - string ready to write to file
     */
    public String finalOutput() {
        String output = "S = ";
        String[] statesOut = states.toArray(new String[states.size()]);
        Arrays.sort(statesOut);
        output += iterateList(statesOut, true);
        
        output += "\nA = ";
        String[] actionsOut = actions.toArray(new String[actions.size()]);
        Arrays.sort(actionsOut);
        output += iterateList(actionsOut, false);
        
        output += "\nT = ";
        Transition[] transitionsOut = transitions.toArray(new Transition[transitions.size()]);
        Arrays.sort(transitionsOut, new Comparator<Transition>() {
            @Override
            public int compare(Transition x, Transition y) {
                if (!x.source.equalsIgnoreCase(y.source))
                    return x.source.compareTo(y.source);

                if (!x.action.equalsIgnoreCase(y.action))
                    return x.action.compareTo(y.action);

                return x.destination.compareTo(y.destination);
            }
        });
        
        Iterator<Transition> it = Arrays.asList(transitionsOut).iterator();
        while (it.hasNext()) {
            output += it.next().transitionOutput();
            output = (it.hasNext() ? output += "," : output);
        }
        
        return output += "\n";
    }
}