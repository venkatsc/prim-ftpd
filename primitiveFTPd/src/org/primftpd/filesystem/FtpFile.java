package org.primftpd.filesystem;

import java.io.File;

import org.apache.ftpserver.ftplet.User;

public class FtpFile extends AndroidFile<org.apache.ftpserver.ftplet.FtpFile>
	implements org.apache.ftpserver.ftplet.FtpFile
{
	private final User user;

	public FtpFile(File file, User user)
	{
		super(file);
		this.user = user;
	}

	@Override
	protected org.apache.ftpserver.ftplet.FtpFile createFile(File file)
	{
		return new FtpFile(file, user);
	}

	@Override
	public String getOwnerName() {
		logger.debug("getOwnerName()");
		return user.getName();
	}

	@Override
	public String getGroupName() {
		logger.debug("getGroupName()");
		return user.getName();
	}

	@Override
	public boolean move(org.apache.ftpserver.ftplet.FtpFile target)
	{
		return move(target);
	}

	public User getUser() {
		return user;
	}
}