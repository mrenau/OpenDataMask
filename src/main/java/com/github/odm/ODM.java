package com.github.odm;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;

import com.github.odm.data.DataTable;
import com.github.odm.exception.ConfigException;
import com.github.odm.model.Schema;
import com.github.odm.model.Table;
import com.github.odm.util.QueryUtil;

public class ODM {

	public static void main(String[] args) {
		try {
			new ODM().run(args);
		} catch (ParseException e) {
			HelpFormatter help = new HelpFormatter();

			help.printHelp("java -jar odm.jar", getCommandOptions(), true);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void run(String[] args) throws SQLException, ClassNotFoundException,
			InstantiationException, IllegalAccessException, ParseException {

		Options options = getCommandOptions();

		PosixParser parser = new PosixParser();
		CommandLine cmd = parser.parse(options, args);
		// run(new File(cmd.getOptionValue("c")));
	}

	public void run(File config, Connection orignConn, Connection destConn)
			throws SQLException, IOException, ConfigException {

		MetadataReader metadataReader = new MetadataReader(orignConn);
		Schema schema = metadataReader.buildSchema();
		
		ConfigManager configManager = new ConfigManager(config);
		configManager.apply(schema);

		for (Table table : schema.getTables()) {
			QueryUtil.delete(destConn, table);
		}

		for (Table table : schema.getTables()) {

			ResultSet result = QueryUtil.select(orignConn, table);
			while (result.next()) {
				DataTable data = new DataTable(table);
				data.fetch(result);
				data.mask();

				QueryUtil.insert(destConn, data);
			}
		}

	}

	/**
	 * <p>
	 * Get the command line options
	 * </p>
	 * 
	 * @return the command line options
	 */
	private static Options getCommandOptions() {
		Options options = new Options();

		Option xmlOpt = new Option("c", "xml configuration file");
		xmlOpt.setRequired(true);
		xmlOpt.setArgs(1);

		options.addOption(xmlOpt);

		return options;
	}

}
