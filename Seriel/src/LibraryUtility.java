import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
 
public class LibraryUtility {
  
  public static final String libraryName = "win32com";
  
  /**
   * �ˬd�禡�w�O�_�s�b
   * @return �禡�w�O�_�s�b
   */
  public static boolean libraryExist(){
    return libraryExist(false);
  }
  
  /**
   * �ˬd�禡�w�O�_�s�b
   * @param install_on_not_exist ��禡�w���s�b��,�O�_�j��ӤJ
   * @return �禡�w�O�_�s�b
   */
  public static boolean libraryExist(boolean install_on_not_exist){
    try {
      System.loadLibrary(libraryName);
      return true;
    } catch (java.lang.UnsatisfiedLinkError e) {
      //��Library���s�b��,�j��g�J
      if(install_on_not_exist){
        return libraryInstall();
      }
      return false;
    }
  }
 
  /**
   * �N�禡�w�ȤJ�t��
   * @return �O�_���\
   */
  public static boolean libraryInstall(){
    try {
      String libraryPath = System.getProperty("java.library.path").split(";")[0];
 
      InputStream fis = LibraryUtility.class.getResource(libraryName+".dll").openStream();
      FileOutputStream fos = new FileOutputStream(libraryPath+"/"+libraryName+".dll");
      
      while(true){
        int read = fis.read();
        if(read==-1){
          break;
        }
        fos.write(read);
      }
      fos.close();
      fis.close();
      return true;
    } catch (IOException e) {
      return false;
    } catch (Exception e){
      return false;
    }
  }
}

