Quarks2 {
	classvar cachepath, sourceslistpath, sourceslist, cupboardpath;

	*initClass {
		StartUp.add{
			sourceslistpath = Platform.userAppSupportDir +/+ "quarks_sources.txt";
			cachepath       = Platform.userAppSupportDir +/+ "quarks_infocache";
			cupboardpath    = Platform.userAppSupportDir +/+ "quarks_cupboard";
			/* TEMPORARY: HARDCODED LIST RATHER THAN READING FROM THE .txt WHICH SHOULD BE AT sourceslistpath */
			sourceslist = [
				/* TEMPORARY: the official recommendations should be held remotely (http: not file:) */
				"file:///%/Quarks2/recommendedsources.yaml".format(Platform.userExtensionDir)
			];
		}
	}

	*refresh {
		var tmppath, sources = Dictionary[], landingpath;
		// For each "sourceslist" item, fetch the file somewhere temporary and parse its data into a dictionary of sources (label -> URI)
		sourceslist.do{ |uri|
			tmppath = PathName.tmp +/+ "a_sourceslist.yaml";
			File.delete(tmppath);
			this.fetch(uri, tmppath, singlefile: true);
			tmppath.parseYAMLFile().keysValuesDo{ |sourcelbl, sourceuri|
				landingpath = cachepath +/+ "sources" +/+ sourcelbl ++ ".yaml";
				"rm -rf %".format(landingpath.quote).systemCmd;
				this.fetch(sourceuri, landingpath, singlefile: true);
				// todo: be more careftul for sourcelbl name-clashes and YAML parse failures
			};
		};
		File.delete(tmppath);

	}

	*getQuarksInfo {
		var quarks = Dictionary[], parsed, sourcename;
		// for each source, parse the quarks out (remembering which source it's from) (error if nameclash)
		(cachepath +/+ "sources/*.yaml").pathMatch.do{ |sourcecachepath|
			sourcename = sourcecachepath.basename.splitext[0].asSymbol;
			parsed = sourcecachepath.parseYAMLFile();
			parsed.keysValuesDo{ |quarkname, quarkinfo|
				"quarkINFO".postln;
				quarkinfo.postln;
				if(quarks[quarkname].notNil){
					"getQuarkInfo skipping '%' from source '%' because of nameclash with another quark".format(quarkname, sourcename).warn;
				}{
					quarkinfo.grow; // Seems needed to ensure insertion of new key will succeed (!!)
					quarkinfo["source"] = sourcename;
					quarks[quarkname] = quarkinfo;
				};
			};
		};
		^quarks
	}

	// Downloads a quark from source-->cupboard
	*getQuark { |name, scversion, quarkversion, quarklist|
		var quarkmeta, foldername=this.quarkFolderName(name, scversion, quarkversion), folderpath;
		folderpath = cupboardpath +/+ foldername;

		// Ask the quark what its [method, uri, where] are, then pass them to the *fetch
		quarkmeta = (quarklist ?? {this.getQuarksInfo})[name.asString];
		if(quarkmeta.isNil){ Error("Quark '%' not found in metadata".format(name)).throw };

		//this.fetch(quarkmeta["uri"], folderpath, quarkmeta["method"], quarkmeta["version"][quarkversion]["where"])
		"this.fetch(%, %, %, %)".format(quarkmeta["uri"], folderpath, quarkmeta["method"], quarkmeta["version"][quarkversion.asString]["where"]).postln;
	}

	// Adds a local quark to LanguageConfig, ensuring not a duplicate entry
	*installQuark {|name, scversion, quarkversion|
		var foldername=this.quarkFolderName(name, scversion, quarkversion), folderpath;
		folderpath = cupboardpath +/+ foldername;

		// NOT DONE
	}
	*uninstallQuark {|name, scversion, quarkversion|
		// NOT DONE
	}

	*quarkFolderName {|name, scversion, quarkversion|
		^"%-%-%".format(name, scversion, quarkversion);
		// TODO LATER:
		//   - if scversion unset, use current major version
		//   - if quarkversion unset, use latest compatible
		//   - also somehow allow for unversioned paths (for installing by bare URL with no metadata)
	}

	// Generic method for fetching something from elsewhere. Nothing quark-specific in here.
	*fetch { | uri, path, method, where, singlefile=false |
		// "where" is extra information for retrieving that might not fit in the URI - for git sources, for example, it is a TAG
		// If "path" already exists, this is an "update"-type operation. Otherwise it's a first-time.
		var firstTime = File.exists(path);
		if(method.isNil){
			method = \file;
			if(uri[..3]=="git:"){ method=\git };
		};
		method.switch(
			\file, {
				if(uri[..6] == "file://"){ uri = uri[7..]};
				if(File.exists(uri).not){ 
					Error("File path not found: %".format(uri)).throw;
				};
				//if(firstTime.not){File.delete(uri)};
				File.mkdir(path.dirname);
				File.copy(uri, path);
			},
			{
				Error("Unrecognised fetch method: %".format(method)).throw;
			}
		);
	}

}

