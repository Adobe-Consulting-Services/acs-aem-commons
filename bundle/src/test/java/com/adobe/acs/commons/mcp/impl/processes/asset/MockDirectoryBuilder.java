/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2018 Adobe
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package com.adobe.acs.commons.mcp.impl.processes.asset;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.ChannelSftp.LsEntry;
import com.jcraft.jsch.SftpATTRS;
import java.util.ArrayList;
import java.util.Vector;

import static org.mockito.Mockito.*;

/**
 * Useful in building up a mock directory for SFTP mock testing
 */
public class MockDirectoryBuilder {

    ArrayList<LsEntry> directory = new ArrayList<>();

    ChannelSftp sftp = new ChannelSftp();

    String currentDirectory = "";

    public MockDirectoryBuilder atFolder(String base) {
        currentDirectory = base;
        return this;
    }

    public MockDirectoryBuilder addFile(String name, Long size) {
        String longName = currentDirectory + "/" + name;

        SftpATTRS attrs = mock(SftpATTRS.class);
        when(attrs.isDir()).thenReturn(false);
        when(attrs.getSize()).thenReturn(size);
        LsEntry entry = mock(LsEntry.class);
        when(entry.getFilename()).thenReturn(name);
        when(entry.getAttrs()).thenReturn(attrs);
        directory.add(entry);

        return this;
    }

    public MockDirectoryBuilder addDirectory(String name) {
        String longName = currentDirectory + "/" + name;

        SftpATTRS attrs = mock(SftpATTRS.class);
        LsEntry entry = mock(LsEntry.class);
        when(entry.getFilename()).thenReturn(name);
        directory.add(entry);

        return this;
    }

    public Vector<LsEntry> asVector() {
        return new Vector<>(directory);
    }
}
