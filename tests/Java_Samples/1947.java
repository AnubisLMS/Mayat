package com.custom.utils;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.List;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.os.StatFs;
import com.custom.bean.ResourceBean;
import com.custom.bean.ResourceBean.ResourceType;
import com.custom.utils.Constant.DirType;

public class LoadResources {

    private static final Logger logger = Logger.getLogger(LoadResources.class);

    static boolean secrete = true;

    /**
	 * 根据路径和文件名称获取文件对象
	 * @param foldPath
	 * @param dirType
	 * @return
	 */
    public static File getFileByType(String foldPath, DirType dirType) {
        if (DirType.sd == dirType && Constant.getSdPath() != null) {
            return new File(Constant.getSdPath() + File.separator + foldPath);
        } else if (DirType.file == dirType && Constant.getUpdateDataPath() != null) {
            return new File(Constant.getUpdateDataPath() + File.separator + foldPath);
        } else if (DirType.extSd == dirType && Constant.getExtSdPath() != null) {
            return new File(Constant.getExtSdPath() + File.separator + foldPath);
        }
        return null;
    }

    /**
	 * 根据路径和路径类型获取bitmap
	 * @param context
	 * @param filePath
	 * @param dirType
	 * @return
	 * @throws Exception
	 */
    public static Bitmap loadBitmap(Context context, String filePath, DirType dirType) throws Exception {
        InputStream in = null;
        try {
            byte[] buffer = LoadResources.loadFile(context, filePath, dirType);
            if (buffer != null) {
                BitmapFactory.Options opts = new BitmapFactory.Options();
                Bitmap bm = BitmapFactory.decodeStream(new BufferedInputStream(new ByteArrayInputStream(buffer)), null, opts);
                return bm;
            }
        } catch (Exception e) {
            throw e;
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (Exception e) {
                }
            }
        }
        return null;
    }

    /**
	 * 根据路径和路径类型读取文件
	 * @param context
	 * @param filePath
	 * @param dirType
	 * @return
	 */
    public static byte[] loadFile(Context context, String filePath, DirType dirType) {
        return loadFile(context, filePath, dirType, LoadResources.secrete);
    }

    /**
	 * 根据路径和路径类型读取文件
	 * @param context
	 * @param filePath
	 * @param dirType
	 * @return
	 */
    public static byte[] loadFile(Context context, String filePath, DirType dirType, boolean secrete) {
        InputStream in = null;
        try {
            if (dirType == DirType.assets) {
                AssetManager assetManager = context.getAssets();
                in = assetManager.open(filePath);
            } else if (dirType == DirType.file && Constant.getUpdateDataPath() != null) {
                in = new FileInputStream(Constant.getUpdateDataPath() + File.separator + filePath);
            } else if (dirType == DirType.sd && Constant.getSdPath() != null) {
                in = new FileInputStream(Constant.getSdPath() + File.separator + filePath);
            } else if (dirType == DirType.extSd && Constant.getExtSdPath() != null) {
                in = new FileInputStream(Constant.getExtSdPath() + File.separator + filePath);
            }
            if (in == null) return null;
            byte[] buf = new byte[in.available()];
            if (in.read(buf, 0, buf.length) >= ZipToFile.encrypLength && secrete) {
                byte[] encrypByte = new byte[ZipToFile.encrypLength];
                System.arraycopy(buf, 0, encrypByte, 0, ZipToFile.encrypLength);
                byte[] temp = CryptionControl.getInstance().decryptECB(encrypByte, ZipToFile.rootKey);
                System.arraycopy(temp, 0, buf, 0, ZipToFile.encrypLength);
            }
            return buf;
        } catch (Exception e) {
            return null;
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (Exception e) {
                }
            }
        }
    }

    /**
	 * 根据路径和路径类型读取文件
	 * @param context
	 * @param filePath
	 * @param dirType
	 * @return
	 */
    public static byte[] loadLocalFile(Context context, String filePath, DirType dirType) {
        return loadLocalFile(context, filePath, dirType, LoadResources.secrete);
    }

    /**
	 * 根据路径和路径类型读取本地文件
	 * @param context
	 * @param filePath
	 * @param dirType
	 * @return
	 */
    public static byte[] loadLocalFile(Context context, String filePath, DirType dirType, boolean secrete) {
        InputStream in = null;
        try {
            if (dirType == DirType.assets) {
                AssetManager assetManager = context.getAssets();
                in = assetManager.open(filePath);
            } else if (dirType == DirType.file) {
                in = new FileInputStream(context.getFilesDir().getAbsoluteFile() + File.separator + filePath);
            } else if (dirType == DirType.sd && Constant.getSdPath() != null) {
                in = new FileInputStream(Constant.getSdPath() + File.separator + filePath);
            } else if (dirType == DirType.extSd && Constant.getExtSdPath() != null) {
                in = new FileInputStream(Constant.getExtSdPath() + File.separator + filePath);
            }
            if (in == null) return null;
            byte[] buf = new byte[in.available()];
            if (in.read(buf, 0, buf.length) >= ZipToFile.encrypLength && secrete) {
                byte[] encrypByte = new byte[ZipToFile.encrypLength];
                System.arraycopy(buf, 0, encrypByte, 0, ZipToFile.encrypLength);
                byte[] temp = CryptionControl.getInstance().decryptECB(encrypByte, ZipToFile.rootKey);
                System.arraycopy(temp, 0, buf, 0, ZipToFile.encrypLength);
            }
            return buf;
        } catch (Exception e) {
            return null;
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (Exception e) {
                }
            }
        }
    }

    /**
	 * 根据文件名称，到3个地方去读取文件
	 * @param context
	 * @param filePath
	 * @param dirType
	 * @return
	 */
    public static byte[] loadPrefaceFile(Context context, String filePath) {
        byte[] result = loadFile(context, filePath, DirType.sd);
        if (result == null) result = loadFile(context, filePath, DirType.extSd);
        if (result == null) result = loadFile(context, filePath, DirType.file);
        if (result == null) result = loadFile(context, filePath, DirType.assets);
        return result;
    }

    /**
	 * 保存临时文件
	 * @param context
	 * @param filePath
	 * @param dirType
	 * @return
	 */
    public static boolean saveToTempFile(Context context, byte[] buffer, String tempSavePath) {
        FileOutputStream fos = null;
        boolean result = false;
        if (buffer == null || buffer.length < 1) return result;
        try {
            try {
                File f = new File(context.getFilesDir().getAbsolutePath() + "/" + tempSavePath);
                if (f.exists()) {
                    f.delete();
                }
                context.deleteFile(tempSavePath);
                fos = context.openFileOutput(tempSavePath, Context.MODE_WORLD_READABLE + Context.MODE_WORLD_WRITEABLE);
                fos.write(buffer);
                fos.flush();
                result = true;
            } catch (Exception e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (fos != null) {
                try {
                    fos.flush();
                    fos.close();
                } catch (Exception e) {
                }
            }
        }
        return true;
    }

    /**
	 * 保存临时文件
	 * @param context
	 * @param filePath
	 * @param dirType
	 * @return
	 */
    public static boolean saveToTempFile(Context context, String filePath, DirType dirType, String tempSavePath, boolean secrete) {
        FileOutputStream fos = null;
        InputStream in = null;
        byte[] buffer = new byte[1024];
        int readLength = 0;
        boolean result = false;
        try {
            try {
                File f = new File(context.getFilesDir().getAbsolutePath() + File.separator + tempSavePath);
                if (f.exists()) {
                    context.deleteFile(tempSavePath);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            fos = context.openFileOutput(tempSavePath, Context.MODE_WORLD_READABLE);
            logger.error("tempfile:" + tempSavePath);
            if (dirType == DirType.assets) {
                AssetManager assetManager = context.getAssets();
                in = assetManager.open(filePath);
            } else if (dirType == DirType.file && Constant.getUpdateDataPath() != null) {
                in = new FileInputStream(Constant.getUpdateDataPath() + File.separator + filePath);
            } else if (dirType == DirType.sd && Constant.getSdPath() != null) {
                in = new FileInputStream(Constant.getSdPath() + File.separator + filePath);
            } else if (dirType == DirType.extSd && Constant.getExtSdPath() != null) {
                in = new FileInputStream(Constant.getExtSdPath() + File.separator + filePath);
            }
            if (in == null) {
                return false;
            }
            readLength = in.read(buffer);
            if (readLength >= ZipToFile.encrypLength && secrete) {
                byte[] encrypByte = new byte[ZipToFile.encrypLength];
                System.arraycopy(buffer, 0, encrypByte, 0, ZipToFile.encrypLength);
                byte[] temp = CryptionControl.getInstance().decryptECB(encrypByte, ZipToFile.rootKey);
                System.arraycopy(temp, 0, buffer, 0, ZipToFile.encrypLength);
            }
            while (readLength > 0) {
                fos.write(buffer, 0, readLength);
                fos.flush();
                readLength = in.read(buffer);
            }
            result = true;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (fos != null) {
                try {
                    fos.flush();
                    fos.close();
                } catch (Exception e) {
                }
            }
            if (in != null) {
                try {
                    in.close();
                } catch (Exception e) {
                }
            }
        }
        return result;
    }

    /**
	 * 保存临时文件
	 * @param context
	 * @param filePath
	 * @param dirType
	 * @return
	 */
    public static boolean saveToTempFile(Context context, String filePath, DirType dirType, String tempSavePath) {
        return LoadResources.saveToTempFile(context, filePath, dirType, tempSavePath, LoadResources.secrete);
    }

    public static Bitmap getBitmap(Context context, String filePath) {
        Bitmap bm = null;
        if (bm == null) {
            try {
                bm = LoadResources.loadBitmap(context, filePath, DirType.sd);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (bm == null) {
            try {
                bm = LoadResources.loadBitmap(context, filePath, DirType.extSd);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (bm == null) {
            try {
                bm = LoadResources.loadBitmap(context, filePath, DirType.file);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (bm == null) {
            try {
                bm = LoadResources.loadBitmap(context, filePath, DirType.assets);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return bm;
    }

    /**
	 * 查询已经下载了多少业务
	 */
    public static int queryDownedFold(Context context) {
        String filePath = Constant.path + File.separator + Constant.mapFileName;
        try {
            byte[] buf = null;
            if (Constant.getSdPath() != null && !"".equals(Constant.getSdPath())) {
                buf = LoadResources.loadFile(context, filePath, DirType.sd);
            }
            if (Constant.getExtSdPath() != null && !"".equals(Constant.getExtSdPath())) {
                buf = LoadResources.loadFile(context, filePath, DirType.extSd);
            }
            if (buf == null) {
                buf = LoadResources.loadFile(context, filePath, DirType.file);
            }
            if (buf == null) {
                buf = LoadResources.loadFile(context, filePath, DirType.assets);
            }
            if (buf == null) {
                return 0;
            }
            BufferedReader fin = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(buf)));
            String line = fin.readLine();
            int count = 0;
            while (line != null) {
                line = line.substring(line.indexOf('=') + 1);
                if (line.indexOf("=") > 0) {
                    count++;
                }
                line = fin.readLine();
            }
            return count;
        } catch (Exception e) {
        }
        return 0;
    }

    public static int queryDownedFold(Context context, String foldPath) {
        ScanFoldUtils scan = new ScanFoldUtils(context, foldPath);
        scan.queryRes();
        int count = 0;
        if (!Constant.path.equals(foldPath)) {
            count = scan.resourceInfo.size();
        }
        Iterator it = scan.resourceInfo.keySet().iterator();
        while (it.hasNext()) {
            ResourceBean res = scan.resourceInfo.get(it.next());
            if (res.getRaws() != null && res.getRaws().size() > 0 && res.getRaws().get(0).getType() == ResourceType.fold) {
                count += queryDownedFold(context, res.getRaws().get(0).getRawPath());
            }
        }
        return count;
    }

    /**
	 * 检查对应的apk文件是否安装
	 * 如果已经安装则返回包名
	 * @param apkFile
	 * @return
	 */
    public static String getInstalledPackName(Context context, String apkFile) {
        try {
            PackageManager pm = context.getPackageManager();
            PackageInfo info = pm.getPackageArchiveInfo(apkFile, PackageManager.GET_ACTIVITIES);
            if (info != null) {
                ApplicationInfo appInfo = info.applicationInfo;
                String packageName = appInfo.packageName;
                try {
                    info = pm.getPackageInfo(packageName, 0);
                    return info.packageName;
                } catch (NameNotFoundException e) {
                    return null;
                }
            }
        } catch (Exception e) {
        }
        return null;
    }

    /**
	 * 启动应用
	 * @param context
	 * @param packageName
	 */
    public static void startApp(Context context, String packageName) {
        try {
            PackageManager pm = context.getPackageManager();
            PackageInfo pi = pm.getPackageInfo(packageName, 0);
            Intent resolveIntent = new Intent(Intent.ACTION_MAIN, null);
            resolveIntent.addCategory(Intent.CATEGORY_LAUNCHER);
            resolveIntent.setPackage(pi.packageName);
            List<ResolveInfo> apps = pm.queryIntentActivities(resolveIntent, 0);
            ResolveInfo ri = apps.iterator().next();
            if (ri != null) {
                String className = ri.activityInfo.name;
                Intent intent = new Intent(Intent.ACTION_MAIN);
                intent.addCategory(Intent.CATEGORY_LAUNCHER);
                ComponentName cn = new ComponentName(packageName, className);
                intent.setComponent(cn);
                context.startActivity(intent);
            }
        } catch (Exception e) {
        }
    }

    public static long[] readExtSDCard() {
        String state = Environment.getExternalStorageState();
        File sdcardDir = new File(Constant.getExtSdPath());
        long[] datas = new long[3];
        if (Environment.MEDIA_MOUNTED.equals(state) && sdcardDir.exists()) {
            StatFs sf = new StatFs(sdcardDir.getPath());
            long blockSize = sf.getBlockSize();
            long blockCount = sf.getBlockCount();
            long availCount = sf.getAvailableBlocks();
            logger.error("block大小:" + blockSize + ",block数目:" + blockCount + ",总大小:" + blockSize * blockCount / 1024 + "KB");
            logger.error("可用的block数目：:" + availCount + ",剩余空间:" + availCount * blockSize / 1024 + "KB");
            datas[0] = blockSize * blockCount;
            datas[1] = availCount * blockSize;
            datas[2] = datas[0] - datas[1];
        }
        return datas;
    }

    public static long[] readSDCard() {
        File root = new File(Constant.getSdPath());
        long[] datas = new long[3];
        if (root.exists()) {
            StatFs sf = new StatFs(root.getPath());
            long blockSize = sf.getBlockSize();
            long blockCount = sf.getBlockCount();
            long availCount = sf.getAvailableBlocks();
            logger.error("block大小:" + blockSize + ",block数目:" + blockCount + ",总大小:" + blockSize * blockCount / 1024 + "KB");
            logger.error("可用的block数目：:" + availCount + ",可用大小:" + availCount * blockSize / 1024 + "KB");
            datas[0] = blockSize * blockCount;
            datas[1] = availCount * blockSize;
            datas[2] = datas[0] - datas[1];
        }
        return datas;
    }

    /**
	 * 以下代码没有用到
	 * @param options
	 * @param minSideLength
	 * @param maxNumOfPixels
	 * @return
	 */
    public static int computeSampleSize(BitmapFactory.Options options, int minSideLength, int maxNumOfPixels) {
        int initialSize = computeInitialSampleSize(options, minSideLength, maxNumOfPixels);
        int roundedSize;
        if (initialSize <= 8) {
            roundedSize = 1;
            while (roundedSize < initialSize) {
                roundedSize <<= 1;
            }
        } else {
            roundedSize = (initialSize + 7) / 8 * 8;
        }
        return roundedSize;
    }

    private static int computeInitialSampleSize(BitmapFactory.Options options, int minSideLength, int maxNumOfPixels) {
        double w = options.outWidth;
        double h = options.outHeight;
        int lowerBound = (maxNumOfPixels == -1) ? 1 : (int) Math.ceil(Math.sqrt(w * h / maxNumOfPixels));
        int upperBound = (minSideLength == -1) ? 128 : (int) Math.min(Math.floor(w / minSideLength), Math.floor(h / minSideLength));
        if (upperBound < lowerBound) {
            return lowerBound;
        }
        if ((maxNumOfPixels == -1) && (minSideLength == -1)) {
            return 1;
        } else if (minSideLength == -1) {
            return lowerBound;
        } else {
            return upperBound;
        }
    }

    public static class InputStreamCryp extends InputStream {

        InputStream in = null;

        byte[] header = new byte[ZipToFile.encrypLength];

        int headerLength = 0;

        int readedLength = 0;

        public InputStreamCryp(InputStream in) {
            this.in = in;
            try {
                if ((headerLength = in.read(header)) == ZipToFile.encrypLength) {
                    logger.error("headerLength:" + headerLength + "readedLength:" + readedLength);
                }
            } catch (Exception e) {
                e.printStackTrace();
                headerLength = 0;
            }
        }

        @Override
        public int read() throws IOException {
            if (headerLength > readedLength) {
                logger.error("header[readedLength++]&0xFF:" + readedLength);
                return header[readedLength++] & 0xFF;
            }
            readedLength++;
            return in.read();
        }

        public final boolean markSupported() {
            logger.error("markSupported:");
            return in.markSupported();
        }

        public final int available() throws IOException {
            logger.error("available:");
            return in.available();
        }

        public final void close() throws IOException {
            logger.error("close:");
            in.close();
        }

        public final void mark(int readlimit) {
            logger.error("mark:");
            in.mark(readlimit);
        }

        public final void reset() throws IOException {
            logger.error("reset:");
            in.reset();
        }

        public final int read(byte[] b) throws IOException {
            logger.error("read(byte[] b) :");
            return read(b, 0, b.length);
        }

        public final int read(byte[] b, int off, int len) throws IOException {
            logger.error("len:" + len);
            if (b == null || off > b.length || off + len > b.length) {
                throw new IOException("buff is error");
            }
            int readL = 0;
            if (headerLength > readedLength) {
                readL = len > (headerLength - readedLength) ? (headerLength - readedLength) : len;
                System.arraycopy(header, readedLength, b, off, readL);
            }
            if (readL < len) {
                readL = readL + in.read(b, off + readL, len - readL);
            }
            readedLength += (readL < 0 ? 0 : readL);
            logger.error("read(byte[] b, int off, int len) readedLength" + readedLength);
            return readL;
        }

        public final long skip(long n) throws IOException {
            logger.error("skip:");
            return in.skip(n);
        }

        protected void finalize() throws Throwable {
            logger.error("finalize:");
            close();
        }
    }
}
