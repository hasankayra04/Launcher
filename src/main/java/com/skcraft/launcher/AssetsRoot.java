/*
 * SK's Minecraft Launcher
 * Copyright (C) 2010-2014 Albert Pham <http://www.sk89q.com> and contributors
 * Please see LICENSE.txt for license information.
 */

package com.skcraft.launcher;

import com.google.common.io.Files;
import com.skcraft.concurrency.ProgressObservable;
import com.skcraft.launcher.model.minecraft.Asset;
import com.skcraft.launcher.model.minecraft.AssetsIndex;
import com.skcraft.launcher.model.minecraft.VersionManifest;
import com.skcraft.launcher.persistence.Persistence;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.java.Log;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.logging.Level;

import static com.skcraft.launcher.util.SharedLocale._;

@Log
public class AssetsRoot {

    @Getter
    private final File dir;

    public AssetsRoot(File dir) {
        this.dir = dir;
    }

    public File getIndexPath(VersionManifest versionManifest) {
        return new File(dir, "indexes/" + versionManifest.getAssetsIndex() + ".json");
    }

    public File getObjectPath(Asset asset) {
        String hash = asset.getHash();
        return new File(dir, "objects/" + hash.substring(0, 2) + "/" + hash);
    }

    public AssetsTreeBuilder createAssetsBuilder(@NonNull VersionManifest versionManifest) {
        String indexId = versionManifest.getAssetsIndex();
        AssetsIndex index = Persistence.read(getIndexPath(versionManifest), AssetsIndex.class);
        File treeDir = new File(dir, "virtual/" + indexId);
        treeDir.mkdirs();
        return new AssetsTreeBuilder(index, treeDir);
    }

    public class AssetsTreeBuilder implements ProgressObservable {
        private final AssetsIndex index;
        private final File destDir;
        private final int count;
        private int processed = 0;

        public AssetsTreeBuilder(AssetsIndex index, File destDir) {
            this.index = index;
            this.destDir = destDir;
            count = index.getObjects().size();
        }

        public File build() throws IOException, LauncherException {
            AssetsRoot.log.info("Building asset virtual tree at '" + destDir.getAbsolutePath() + "'...");

            for (Map.Entry<String, Asset> entry : index.getObjects().entrySet()) {
                File objectPath = getObjectPath(entry.getValue());
                File virtualPath = new File(destDir, entry.getKey());
                virtualPath.getParentFile().mkdirs();
                if (!virtualPath.exists()) {
                    log.log(Level.INFO, "Copying {0} to {1}...", new Object[] {
                            objectPath.getAbsolutePath(), virtualPath.getAbsolutePath()});

                    if (!objectPath.exists()) {
                        String message = _("assets.missingObject", objectPath.getAbsolutePath());
                        throw new LauncherException("Missing object " + objectPath.getAbsolutePath(), message);
                    }

                    Files.copy(objectPath, virtualPath);
                }
                processed++;
            }

            return destDir;
        }

        @Override
        public double getProgress() {
            if (count == 0) {
                return -1;
            } else {
                return processed / (double) count;
            }
        }

        @Override
        public String getStatus() {
            if (count == 0) {
                return _("assets.expanding1", count, count - processed);
            } else {
                return _("assets.expandingN", count, count - processed);
            }
        }
    }

}