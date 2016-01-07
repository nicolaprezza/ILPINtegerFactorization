package ilpif;
import ilog.concert.IloException;
import ilog.concert.IloIntVar;
import ilog.concert.IloNumVar;
import ilog.concert.IloNumExpr;
import ilog.cplex.IloCplex;

import java.io.File;
import java.lang.reflect.Field;


public class CPLEXSolver {
	
	static {
		try {
			String workdir = new File(".").getCanonicalPath();
			if (System.getProperty("os.name").toLowerCase().contains("windows")) {
				// try to load Windows library
				if (System.getProperty("os.arch").toLowerCase().contains("64")) {
					System.setProperty("java.library.path", System.getProperty("java.library.path")+":"+workdir+"/os/windows/x86_64");
				}
				else if (System.getProperty("os.arch").toLowerCase().contains("86")) {
					System.setProperty("java.library.path", System.getProperty("java.library.path")+":"+workdir+"/os/windows/x86");
				}
				else {
					System.out.println("The os and/or arch is not supported.");
				}
				
			}
			else if (System.getProperty("os.name").toLowerCase().contains("linux")) {
				// try to load Linux library
				if (System.getProperty("os.arch").toLowerCase().contains("64")) {
					System.setProperty("java.library.path", System.getProperty("java.library.path")+":"+workdir+"/os/linux/x86_64");
				}
				else if (System.getProperty("os.arch").toLowerCase().contains("86")) {
					System.setProperty("java.library.path", System.getProperty("java.library.path")+":"+workdir+"/os/linux/x86");
				}
				else {
					System.out.println("The os and/or arch is not supported.");
				}
			}
			else if (System.getProperty("os.name").toLowerCase().contains("mac")) {
				// try to load Mac library
				if (System.getProperty("os.arch").toLowerCase().contains("64")) {
					System.setProperty("java.library.path", System.getProperty("java.library.path")+":"+workdir+"/os/macosx/x86_64");
				}
				else if (System.getProperty("os.arch").toLowerCase().contains("86")) {
					System.setProperty("java.library.path", System.getProperty("java.library.path")+":"+workdir+"/os/macosx/x86");
				}
				else {
					System.out.println("The os and/or arch is not supported.");
				}
			} 
			else {
				System.out.println("The os and/or arch is not supported.");
			}
			Field fieldSysPath = ClassLoader.class.getDeclaredField( "sys_paths" );
			fieldSysPath.setAccessible( true );
			fieldSysPath.set( null, null );
			System.loadLibrary("cplex125");
		} catch (Exception e) {
			System.out.println("The dynamic link library for CPLEX could not be"
									+ "loaded.\nConsider using\njava -Djava.library.path=");
			e.printStackTrace();
		}
	}

	long N;
	IloCplex solver;
	// funzione obiettivo
	IloNumExpr objFn;
	// primo fattore
	IloIntVar[] qVariables;
	// secondo fattore
	IloIntVar[] pVariables;
	
	// variabili interne alla matrice B
	IloNumVar[][] B;
	
	int nrOfqVar;
	int nrOfpVar;
	
	public CPLEXSolver(long N){
		this.N=N;
		long N_temp=N;
		nrOfpVar=0;

		while(N_temp>0){
			N_temp /=2;
			nrOfpVar++;
		}
		
		nrOfqVar = nrOfpVar/2 + 1;
		
		System.out.println("numero di variabili q = " + nrOfqVar);
		System.out.println("numero di variabili p = " + nrOfpVar);
		System.out.println("numero di variabili intere = p+q = " + (nrOfpVar+nrOfqVar));
		System.out.println("numero di variabili non intere = " + ((nrOfpVar-1)*(nrOfqVar-1)));

	}
	
	public long solve() throws IloException{
		
		this.solver = new IloCplex();
		this.solver.setName("Fattorizzazione di numeri interi PLIM");
		
		//inizializza variabili q
		qVariables = solver.boolVarArray(nrOfqVar);
		for (int i=0; i<nrOfqVar; i++)
			qVariables[i].setName("q_"+(nrOfqVar-i-1));
			
		//inizializza variabili p
		pVariables = solver.boolVarArray(nrOfpVar);
		for (int i=0; i<nrOfpVar; i++)
			pVariables[i].setName("p_"+(nrOfpVar-i-1));
			
		//inizializza variabili in B
		B = new IloNumVar[nrOfqVar][nrOfpVar];
		for(int i=0;i<nrOfqVar;i++)
			for(int j=0;j<nrOfpVar;j++){
				B[i][j] = solver.numVar(0,1);
				B[i][j].setName("B_"+(nrOfqVar-i-1)+"_"+(nrOfpVar-j-1));
			}
		
		//funzione obiettivo
		int ctr=0;
		IloNumExpr vals[] = new IloNumExpr[nrOfqVar*nrOfpVar];
		objFn = solver.constant(0);
		for (int i=0; i<nrOfqVar; i++){
			for (int j=0; j<nrOfpVar; j++){
				
				vals[ctr] = solver.prod(Math.pow(2, i+j), getBVar(i,j));
				ctr++;
					
			}
		}
		objFn = solver.sum(vals);
		solver.addMaximize(objFn);
		
		//VINCOLO 1) somma pesata degli elementi in B non deve eccedere N
		
		ctr=0;
		IloNumExpr Bvals[] = new IloNumExpr[nrOfqVar*nrOfpVar];
		for (int i=0; i<nrOfqVar; i++){
			for (int j=0; j<nrOfpVar; j++){

				Bvals[ctr] = solver.prod(Math.pow(2, i+j), getBVar(i,j));
				ctr++;	
				
			}
		}
		solver.addLe(solver.sum(Bvals), N, "sumLeqN");
		
		//VINCOLI 3) - 4) elementi di B sono il prodotto dei rispettivi elementi di p,q
		
		for (int i=0; i<nrOfqVar; i++){
			for (int j=0; j<nrOfpVar; j++){
				// B_i_j <= q_i
				IloNumExpr pair1[] = new IloNumExpr[2];
				pair1[0] = getBVar(i,j);
				pair1[1] = solver.prod(-1, getqVar(i));
				solver.addLe(solver.sum(pair1), 0, "B_"+i+"_"+j+"<= q_"+i);
				// B_i_j <= p_j
				IloNumExpr pair2[] = new IloNumExpr[2];
				pair2[0] = getBVar(i,j);
				pair2[1] = solver.prod(-1, getpVar(j));
				solver.addLe(solver.sum(pair2), 0, "B_"+i+"_"+j+"<= p_"+j);
				// 1+B_i_j >= q_i + p_j -> q_i + p_j - B_i_j <= 1
				IloNumExpr triple[] = new IloNumExpr[3];
				triple[0] = solver.prod(-1, getBVar(i,j));
				triple[1] = getqVar(i);
				triple[2] = getpVar(j);
				solver.addLe(solver.sum(triple), 1, "1+B_"+i+"_"+j+">= q_"+i+"+p_"+j);
			}
		}
		
		//VINCOLO 5) 1 e N non sono fattori ammessi: somma bit oltre al primo di p e q deve essere >= 1
		IloNumExpr not1q[] = new IloNumExpr[nrOfqVar-1];
		for (int i=1; i<nrOfqVar; i++)
			not1q[i-1]=getqVar(i);
		
		solver.addGe(solver.sum(not1q), 1, "sum(q[1..]) >= 1");
		
		IloNumExpr not1p[] = new IloNumExpr[nrOfpVar-1];
		for (int i=1; i<nrOfpVar; i++)
			not1p[i-1]=getpVar(i);
		
		solver.addGe(solver.sum(not1p), 1, "sum(p[1..]) >= 1");
		
		//Risolvi il sistema

		solver.setParam(IloCplex.DoubleParam.EpGap, 0);
		boolean ret = solver.solve();
		
		printB();
		
		long solution=0;
		
		if(ret){
			for(int i=0;i<nrOfqVar;i++)
				solution = solution*2 + (long)(solver.getValue(qVariables[i]));	

			solver.end();
			return solution;
		}
		
		solver.end();
		return 1;//errore
		
	}
	
	private void printB(){
		
		long sum=0;
		
		try{
						
			for(int y=0;y<nrOfpVar;y++){
				for(int x=0;x<nrOfqVar;x++){
					System.out.print((int)solver.getValue(B[x][y])+" ");
					sum += Math.pow(2, nrOfqVar-x-1 + nrOfpVar-y-1)*(long)solver.getValue(B[x][y]);
				}
				System.out.println();
			}
			
			System.out.println("Somma interna pesata = " + sum);
			
		}catch(Exception e){
			
			e.printStackTrace();
			
		}
			
			
	}
	/**
	 * 
	 * @param i
	 * @return variabile q avente come coefficiente 2^i
	 */
	private IloIntVar getqVar(int i){return qVariables[nrOfqVar-i-1];}
	/**
	 * 
	 * @param i
	 * @return variabile p avente come coefficiente 2^i
	 */
	private IloIntVar getpVar(int i){return pVariables[nrOfpVar-i-1];}
	
	/**
	 * 
	 * @param x
	 * @param y
	 * @return variabile in B corrispondente alle potenze 2^x e 2^y rispettivamente nelle colonne e righe 
	 */
	private IloNumVar getBVar(int x, int y){return B[nrOfqVar-x-1][nrOfpVar-y-1];	}
	
	
}
