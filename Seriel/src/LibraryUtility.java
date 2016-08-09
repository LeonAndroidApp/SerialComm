import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
 
public class LibraryUtility {
  
  public static final String libraryName = "win32com";
  
  /**
   * 檢查函式庫是否存在
   * @return 函式庫是否存在
   */
  public static boolean libraryExist(){
    return libraryExist(false);
  }
  
  /**
   * 檢查函式庫是否存在
   * @param install_on_not_exist 當函式庫不存在時,是否強制植入
   * @return 函式庫是否存在
   */
  public static boolean libraryExist(boolean install_on_not_exist){
    try {
      System.loadLibrary(libraryName);
      return true;
    } catch (java.lang.UnsatisfiedLinkError e) {
      //當Library不存在時,強制寫入
      if(install_on_not_exist){
        return libraryInstall();
      }
      return false;
    }
  }
 
  /**
   * 將函式庫值入系統
   * @return 是否成功
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

