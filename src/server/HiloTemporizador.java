package server;

/**
 *
 * @author mivap
 */
public class HiloTemporizador extends Thread{
    
    HiloUsuario hiloUsuario;
    int tiempoEspera=0;
    
    HiloTemporizador(HiloUsuario hiloUsuario){
        this.hiloUsuario=hiloUsuario;
    }
    
    @Override
    public void run(){
        
        do{
            
            try {
                Thread.sleep(10000);
                tiempoEspera+=10000;
            } catch (InterruptedException ex) {
                tiempoEspera=0;
            }
            
            if(!hiloUsuario.isAlive())
                break;
            
            
        }while(tiempoEspera<=1800000);
        System.out.println("Fin hilo temporizador");
        
        if(hiloUsuario.isAlive())
            hiloUsuario.interrupt();
        
    }
}
