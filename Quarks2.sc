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
					"getQuarksInfo skipping '%' from source '%' because of nameclash with another quark".format(quarkname, sourcename).warn;
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
	*fetchQuark { |name, scversion, quarkversion, quarklist|
		var quarkmeta, foldername, folderpath;
		scversion = this.checkSCVersion(scversion);
		quarkversion = this.checkQuarkVersion(name, scversion, quarkversion);
		foldername = this.quarkFolderName(name, scversion, quarkversion);
		folderpath = cupboardpath +/+ foldername;

		// Ask the quark what its [method, uri, fetchInfo] are, then pass them to the *fetch
		quarkmeta = (quarklist ?? {this.getQuarksInfo})[name.asString];
		if(quarkmeta.isNil){ Error("Quark '%' not found in metadata".format(name)).throw };

		//this.fetch(quarkmeta["uri"], folderpath, quarkmeta["method"], quarkmeta["version"][quarkversion]["fetchInfo"])
		"this.fetch(%, %, %, %)".format(quarkmeta["uri"], folderpath, quarkmeta["method"], quarkmeta["version"][quarkversion.asString]["fetchInfo"]).postln;
		this.fetch(quarkmeta["uri"], folderpath, quarkmeta["method"], quarkmeta["version"][quarkversion.asString]["fetchInfo"])
	}

	*cupboardPathForQuark { |name, scversion, quarkversion|
		var foldername;
		scversion = this.checkSCVersion(scversion);
		quarkversion = this.checkQuarkVersion(name, scversion, quarkversion);
		foldername = this.quarkFolderName(name, scversion, quarkversion);
		^cupboardpath +/+ foldername;
	}

	// Adds a local quark to LanguageConfig, ensuring not a duplicate entry
	*install {|name, scversion, quarkversion|
		var folderpath = this.cupboardPathForQuark(name, scversion, quarkversion);
		LanguageConfig.addIncludePath( folderpath );
		LanguageConfig.store;
	}
	*uninstall {|name, scversion, quarkversion|
		var folderpath = this.cupboardPathForQuark(name, scversion, quarkversion);
		LanguageConfig.removeIncludePath( folderpath );
		LanguageConfig.store;
	}

	*quarkFolderName {|name, scversion, quarkversion|
		scversion = this.checkSCVersion(scversion);
		quarkversion = this.checkQuarkVersion(name, scversion, quarkversion);
		^"%-%-%".format(name, scversion, quarkversion );
		// TODO LATER:
		//   - also somehow allow for unversioned paths (for installing by bare URL with no metadata)
	}

	// Generic method for fetching something from elsewhere. Nothing quark-specific in here.
	*fetch { | uri, path, method, fetchInfo, singlefile=false |
		// "fetchInfo" is extra information for retrieving that might not fit in the URI - for git sources, for example, it gives the TAG (if not present, assume it's the version#)
		// If "path" already exists, this is an "update"-type operation. Otherwise it's a first-time.
		var firstTime = File.exists(path).not;
		var escapedPath = path.shellQuote;
		var escapedUri  = uri.shellQuote;
		method = (method ?? { uri.split($:).at(0) }).asSymbol;
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
			\http, {
				var cmd = "curl % -o %".format(escapedUri, escapedPath); // dependency: curl
				cmd.systemCmd
			},
			\git, {
				if( firstTime) {
					("git clone "++escapedUri++" "++escapedPath
						++ (fetchInfo !? { |tag|
							(" && cd "++escapedPath++" && git checkout "++tag)
							} ?? ""
					) ).postln.runInTerminal;
				} {
					("cd" + escapedPath + "&& git fetch"
						++ (fetchInfo !? { |tag|
							(" && git checkout "++tag)
							} ?? ""
					) ).postln.runInTerminal;
				}
			},
			\svn, {
				if( firstTime) {
					("svn checkout" + escapedUri + escapedPath).postln.runInTerminal;
				} {
					("svn update" + escapedPath).postln.runInTerminal;
				}
			},
			{
				Error("Unrecognised fetch method: %".format(method)).throw;
			}
		);
	}

	*checkSCVersion { |scversion|
		^scversion ?? { "%.%".format(Main.scVersionMajor, Main.scVersionMinor); }
	}

	*checkQuarkVersion { |name, scversion, quarkversion|

		quarkversion = quarkversion ?? {
			this.getQuarksInfo[name]["version"]
			.collect{ |x|
				var compat = x["compat"];
				compat.isNil or: {
					(compat.asArray.size == 0) or: { compat.includesEqual(scversion) }
				}
			}
			.select{ |x| x }
			.keys.asArray.sort.last
		};
		if( quarkversion.isNil ) {
			Error("There is no version of the quark % compatible with the current SuperCollider Version:  %".format(name, scversion) ).throw
		};
		^quarkversion
	}

}

