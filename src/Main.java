package ilpif;
import ilpif.CPLEXSolver;
import ilog.concert.IloException;
import java.util.Random;




public class Main {

	public static void main(String[] args) {

		if(args[0].equals("-b"))
			benchmark();
		else if(args[0].equals("-n")){
			
			long time = System.currentTimeMillis();
	
			long N = Integer.parseInt(args[1]);
			
			long p = factorize(N);
			
			if(p*(N/p) != N){
				System.out.println("Fattorizzazione fallita. Fattore errato di "+N + " : " + p);
			}else{
				System.out.println("Numero fattorizzato con successo! " + N + " = " + p + " x " + (N/p));
			}
			System.out.println("Computation time: " + (System.currentTimeMillis()-time) + " ms");
		}else{
			System.out.println("Errore. opzioni accettate: -b, -n <int>");
		}
				
	}//main
		
	/**
	 * lancia il modello su un certo numero di istanze casuali e per ogni lunghezza in base 2 di N salva il risultato peggiore
	 */
	public static void benchmark(){
		
		long inf,sup;
		int maxIter = 100;
		int maxbits = 21;
		Random r = new Random(System.currentTimeMillis());
		long N,p,start,time;
		int bits;
		
		int times[] = new int[maxbits];//worst times
		long instances[] = new long[maxbits];//most difficult instances
		
		for(int i=0;i<maxbits;i++){
			times[i] = -1;
			instances[i] = 0;
		}
		
		for(int j=3;j<=maxbits;j++){
			
			inf=(long)Math.pow(2, j-1)+1;
			sup=(long)Math.pow(2, j);
			
			for(int i=0;i<maxIter;i++){
				
				N = Math.abs(r.nextInt()%(sup-inf))+inf;
				
				bits = (int)Math.ceil(Math.log(N)/Math.log(2));
				
				start = System.currentTimeMillis();
				p = factorize(N);
				time = System.currentTimeMillis()-start;
				
				if(p*(N/p) == N){
					
					if((int)time>times[bits-1]){//se ci ho messo più tempo
						instances[bits-1] = N;
						times[bits-1] = (int)time;
					}
					
				}
				
			}//for
		}//for
		
		for(int i = 2;i<maxbits;i++)
			System.out.println((i+1)+"\t"+ times[i] +"\t"+instances[i]);
		
	}
	
	public static long factorize(long N){
		
		CPLEXSolver solver = new CPLEXSolver(N);

		long p=1;
		
		try{
			p = solver.solve();
		}catch(IloException e){
			e.printStackTrace();
			p=N+1;
		}
		
		if(p==0)
			p=N+1;
		
		return p;
		
	}//factorize

}
