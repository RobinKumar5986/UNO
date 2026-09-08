package com.kgjr.uno.data;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.kgjr.uno.models.project.Project;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * File-backed store for saved projects: one folder, filesDir/projects, holding a
 * &lt;uuid&gt;.json per project and a matching &lt;uuid&gt;.png thumbnail.
 */
public final class ProjectRepository {

    private static final String TAG = "ProjectRepository";
    private static final String DIR = "projects";
    private static final String JSON_SUFFIX = ".json";
    private static final String PNG_SUFFIX = ".png";

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Charset UTF8 = Charset.forName("UTF-8");

    private ProjectRepository() {
    }

    public static File directory(Context context) {
        File dir = new File(context.getFilesDir(), DIR);
        if (!dir.exists() && !dir.mkdirs()) Log.w(TAG, "Could not create " + dir);
        return dir;
    }

    public static File jsonFile(Context context, String id) {
        return new File(directory(context), id + JSON_SUFFIX);
    }

    public static File thumbnailFile(Context context, String id) {
        return new File(directory(context), id + PNG_SUFFIX);
    }

    /**
     * Writes the project, stamping updatedAt. Goes through a temp file that is renamed into
     * place, so a crash mid-save leaves the previous version intact rather than a partial one.
     */
    public static boolean save(Context context, Project project) {
        if (project == null || project.id == null) return false;

        project.updatedAt = System.currentTimeMillis();
        if (project.createdAt == 0L) project.createdAt = project.updatedAt;

        File target = jsonFile(context, project.id);
        File temp = new File(target.getPath() + ".tmp");

        Writer writer = null;
        try {
            writer = new OutputStreamWriter(new FileOutputStream(temp), UTF8);
            GSON.toJson(project, writer);
            writer.flush();
            close(writer);
            writer = null;

            if (!temp.renameTo(target)) {
                Log.e(TAG, "Could not rename " + temp + " to " + target);
                return false;
            }
            return true;

        } catch (IOException | RuntimeException e) {
            Log.e(TAG, "Failed to save project " + project.id, e);
            return false;
        } finally {
            close(writer);
            if (temp.exists() && !temp.delete()) Log.w(TAG, "Left behind " + temp);
        }
    }

    /** A null bitmap leaves any existing thumbnail alone. */
    public static boolean saveThumbnail(Context context, String id, @Nullable Bitmap bitmap) {
        if (id == null || bitmap == null) return false;

        FileOutputStream out = null;
        try {
            out = new FileOutputStream(thumbnailFile(context, id));
            return bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
        } catch (IOException e) {
            Log.e(TAG, "Failed to save thumbnail for " + id, e);
            return false;
        } finally {
            close(out);
        }
    }

    @Nullable
    public static Project load(Context context, String id) {
        if (id == null) return null;
        return read(jsonFile(context, id));
    }

    /** Every saved project, newest edit first. Unreadable files are skipped, not fatal. */
    public static List<Project> loadAll(Context context) {
        List<Project> projects = new ArrayList<>();

        File[] files = directory(context).listFiles();
        if (files == null) return projects;

        for (File file : files) {
            if (!file.isFile() || !file.getName().endsWith(JSON_SUFFIX)) continue;

            Project project = read(file);
            if (project != null && project.id != null) projects.add(project);
        }

        Collections.sort(projects, new Comparator<Project>() {
            @Override
            public int compare(Project a, Project b) {
                return Long.compare(b.updatedAt, a.updatedAt);
            }
        });
        return projects;
    }

    /** Downsampled to roughly the requested size, since list rows are far smaller than the file. */
    @Nullable
    public static Bitmap loadThumbnail(Context context, String id, int targetWidth,
                                       int targetHeight) {
        File file = thumbnailFile(context, id);
        if (!file.exists()) return null;

        try {
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(file.getPath(), bounds);

            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = sampleSize(bounds, targetWidth, targetHeight);
            return BitmapFactory.decodeFile(file.getPath(), options);

        } catch (OutOfMemoryError e) {
            Log.w(TAG, "Thumbnail too large for " + id, e);
            return null;
        }
    }

    public static boolean delete(Context context, String id) {
        if (id == null) return false;

        File json = jsonFile(context, id);
        File png = thumbnailFile(context, id);

        boolean removed = !json.exists() || json.delete();
        if (png.exists() && !png.delete()) Log.w(TAG, "Could not delete thumbnail for " + id);
        return removed;
    }

    public static boolean exists(Context context, String id) {
        return id != null && jsonFile(context, id).exists();
    }

    @Nullable
    private static Project read(File file) {
        if (!file.exists()) return null;

        Reader reader = null;
        try {
            reader = new InputStreamReader(new FileInputStream(file), UTF8);
            Project project = GSON.fromJson(reader, Project.class);
            if (project == null) return null;

            // Tolerate hand-edited or partially written files.
            if (project.nodes == null) project.nodes = new ArrayList<>();
            if (project.connections == null) project.connections = new ArrayList<>();
            if (project.sensorNames == null) project.sensorNames = new ArrayList<>();
            if (project.name == null) project.name = "";
            if (project.description == null) project.description = "";
            if (project.sourceCode == null) project.sourceCode = "";
            if (project.generatedCode == null) project.generatedCode = "";
            return project;

        } catch (IOException | RuntimeException e) {
            Log.e(TAG, "Failed to read " + file, e);
            return null;
        } finally {
            close(reader);
        }
    }

    private static int sampleSize(BitmapFactory.Options bounds, int targetWidth, int targetHeight) {
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return 1;
        if (targetWidth <= 0 || targetHeight <= 0) return 1;

        int sample = 1;
        while (bounds.outWidth / (sample * 2) >= targetWidth
                && bounds.outHeight / (sample * 2) >= targetHeight) {
            sample *= 2;
        }
        return sample;
    }

    private static void close(@Nullable Closeable closeable) {
        if (closeable == null) return;
        try {
            closeable.close();
        } catch (IOException ignored) {
        }
    }
}
