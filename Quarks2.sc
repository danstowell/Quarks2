/*
* Quarks redesign, based on plan discussed at sc2012.
* NOTE: NO gui code in here please (separate file, core having no gui dependence).
*/
Quarks2 {
	classvar <cachepath, <sourceslistpath, <sourceslist, <cupboardpath;

	*initClass {
		StartUp.add{
			sourceslistpath = Platform.userAppSupportDir +/+ "quarks_sources.txt";
			cachepath       = Platform.userAppSupportDir +/+ "quarks_infocache";
			cupboardpath    = Platform.userAppSupportDir +/+ "quarks_cupboard";
			if(File.exists(sourceslistpath).not){
				var fp;
				// Create default sourceslist
				fp = File.open(sourceslistpath, "w");
				fp.write("# sources list - one URL per line. Lines beginning with # are ignored.\nhttps://raw.github.com/supercollider/quarks2seed/master/default_quarks_sources.yaml");
				fp.close();
			};
			sourceslist = File.open(sourceslistpath, "r").readAllString.split(Char.nl);
		}
	}

	*refresh {
		var tmppath, sources = Dictionary[], landingpath, listcount=0;
		if(File.exists(cachepath +/+ "sources").not){ File.mkdir(cachepath +/+ "sources") };
		if(File.exists(cupboardpath           ).not){ File.mkdir(cupboardpath           ) };
		// For each "sourceslist" item, fetch the file somewhere temporary and parse its data into a dictionary of sources (label -> URI)
		sourceslist.do{ |uri|
			if(uri.size!=0 and: {uri[0] != $#}){
				tmppath = PathName.tmp +/+ "a_sourceslist.yaml";
				File.delete(tmppath);
				this.fetch(uri, tmppath, singlefile: true);
				tmppath.parseYAMLFile().keysValuesDo{ |sourcelbl, sourceuri|
					landingpath = cachepath +/+ "sources" +/+ sourcelbl ++ ".yaml";
					"rm -rf %".format(landingpath.quote).systemCmd;
					try{
						this.fetch(sourceuri, landingpath, singlefile: true);
						listcount = listcount + 1;
					}{
						"WARNING: Quarks2 unable to fetch metadata from '%' - some quarks may be unavailable".format(sourceuri).postln;
					};
					// todo: be more careftul for sourcelbl name-clashes and YAML parse failures
				};
			};
		};
		File.delete(tmppath);
		"Quarks2 refreshed from % sourceslists".format(listcount).postln;
	}

	*getQuarksInfo {
		var quarks = Dictionary[], parsed, sourcename;
		// for each source, parse the quarks out (remembering which source it's from) (error if nameclash)
		(cachepath +/+ "sources/*.yaml").pathMatch.do{ |sourcecachepath|
			sourcename = sourcecachepath.basename.splitext[0].asSymbol;
			parsed = sourcecachepath.parseYAMLFile();
			parsed.keysValuesDo{ |quarkname, quarkinfo|
				if(quarks[quarkname].notNil){
					"getQuarksInfo skipping '%' from source '%' because of nameclash with another quark".format(quarkname, sourcename).warn;
				}{
					quarkinfo.grow; // Seems needed to ensure insertion of new key will succeed (!!)
					quarkinfo["source"] = sourcename;
					quarks[quarkname] = this.pr_standardiseDictionaryKeys(quarkinfo);
				};
			};
		};
		^quarks
	}

	// Downloads a quark from source-->cupboard
	*fetchQuark { |name, scversion, quarkversion, quarklist|
		var quarkmeta, foldername, folderpath, quarkversioninfo, fetchinfo;
		quarklist = quarklist ?? {this.getQuarksInfo};
		name = name.asString;
		foldername=this.quarkFolderName(name, scversion, quarkversion);
		folderpath = cupboardpath +/+ foldername;

		// Ask the quark what its [method, uri, fetchInfo] are, then pass them to the *fetch
		quarkmeta = quarklist[name];
		if(quarkmeta.isNil){ Error("Quark '%' not found in metadata".format(name)).throw };

		quarkversioninfo = this.pr_chooseBestVersion(name, scversion, quarkversion, quarklist);
		if(quarkversioninfo.notNil and: {quarkversioninfo["fetchInfo"].notNil}){
			fetchinfo = quarkversioninfo["fetchInfo"];
		};

		this.fetch(quarkmeta["uri"], folderpath, quarkmeta["method"], quarkversioninfo["fetchInfo"])
	}

	// Adds a local quark to LanguageConfig, ensuring not a duplicate entry
	*install {|name, scversion, quarkversion|
		var foldername=this.quarkFolderName(name, scversion, quarkversion), folderpath, existing;
		name = name.asString;
		if(quarkversion.notNil){quarkversion = quarkversion.asFloat};

		existing = this.installed[name];
		if(existing.notNil and: {existing[\quarkversion] != quarkversion }){
			if(existing[\quarkversion].isNil or: {quarkversion.isNil or: {existing[\quarkversion]<quarkversion}}){
				"Quark '%': uninstalling version % in order to install version %"
				.format(name,  existing[\quarkversion], quarkversion).postln;
				this.uninstall(name, scversion, quarkversion);
			}{
				Error("Quark '%' version % cannot be installed: already installed with version %. Uninstall the other version if needed.".format(name, quarkversion, existing[\quarkversion])).throw;
			};
		};

		folderpath = cupboardpath +/+ foldername;

		if(File.exists(folderpath).not){
			this.fetchQuark(name, scversion, quarkversion)
		};

		if(File.exists(folderpath).not){
			Error("Unable to install quark: fetch already attempted, thus expecting a folder at '%'".format(folderpath)).throw;
		};

		LanguageConfig.addIncludePath( folderpath );
		LanguageConfig.store;
		"Quark '%' installed to LanguageConfig".format(name).postln;
	}

	*uninstall {|name, scversion, quarkversion|
		var foldername=this.quarkFolderName(name, scversion, quarkversion), folderpath, installed;
		folderpath = cupboardpath +/+ foldername;

		if(LanguageConfig.includePaths.includesEqual(folderpath)){
			LanguageConfig.removeIncludePath( folderpath );
			LanguageConfig.store;
		}{
			installed = this.installed[name];
			if(installed.isNil){
				"Warning: Quarks2.uninstall - % is not installed, so cannot uninstall".format(name).postln;
			}{
				"Warning: Quarks2.uninstall - % version % is installed, but uninstall version % requested. Not uninstalling.".format(name, installed["quarkversion"], quarkversion).postln;
			};
		};
	}

	*installed {
		^LanguageConfig.includePaths.select(_.beginsWith(Quarks2.cupboardpath))
		.collectAs({ |path|
			var bits = path.basename.findRegexp("^(.+)-(.+?)-(.+?)$");
			var quarkversion = bits[3][1];
			quarkversion = if(quarkversion=="latest"){nil}{quarkversion.asFloat};
			Association(bits[1][1], (scversion: bits[2][1].asFloat, quarkversion: quarkversion))
		}, Dictionary)
	}

	/////////////////////////////////////////////////////////////////////
	// main methods above, "accessory" methods below

	*quarkFolderName {|name, scversion, quarkversion|
		name = name.asString;
		scversion = this.pr_scversionString(scversion);
		quarkversion = (quarkversion ? 0).asFloat;
		// Note: quarkversion is a float for fixed version number, or nil/0 it means "always want the latest compatible"
		^"%-%-%".format(name, scversion, if(quarkversion>0){quarkversion}{"latest"});
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
		if(method==\https){method=\http};
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

	*pr_standardiseDictionaryKeys{ |dict, floatKey=false|
		// for quark purposes, we need to recursively coerce all keys to be str
		//   - except if key is version, subkeys are float
		var result = Dictionary.new(dict.size);
		dict.keysValuesDo{|k,v|
			k = if(floatKey){k.asFloat}{k.asString};
			if(result[k].notNil){ Error("Duplicate key '%' encountered".format(k)).throw };
			if(v.isKindOf(Dictionary)){ v = this.pr_standardiseDictionaryKeys(v, k=="version") };
			result[k]=v;
		};
		^result
	}

	*pr_scversionString{ |scversion|
		^(scversion ?? { "%.%".format(Main.scVersionMajor, Main.scVersionMinor) }).asString
	}

	*pr_chooseBestVersion{ | name, scversion, quarkversion, quarklist |
		// Decide which version should be fetched/installed. Returns a version struct, or nil for unversioned, or error for incompatible.
		// This decision shouldn't affect the foldername btw, since it never returns "latest", always a specific version.
		var candidates, winner;
		scversion = this.pr_scversionString(scversion);
		if(quarklist[name]["version"].isNil){
			if(quarkversion.isNil){
				^nil // if quark entry knows no version, then nil is OK
			}{
				Error("Quark % has no version information, but version % requested".format(name, quarkversion)).throw
			};
		}{
			if(quarkversion.notNil and: {quarklist[name]["version"][quarkversion].isNil}){
				Error("Version '%' not listed in metadata for Quark '%'".format(quarkversion, name)).throw;
			}
		};
		// get a filtered list of the versions compat with this version
		candidates = quarklist[name]["version"].select{|ver| ver["compat"].isNil
			or: {ver["compat"].includesEqual(scversion)}};
		if(candidates.size==0){ Error("Found no compatible version of quark % with %".format(name, scversion)).throw };
		if(quarkversion.notNil){ // locate in filtered list, error if not
			winner = candidates[quarkversion];
			if(winner.isNil){ Error("Quark % version % not marked as being compatible with %".format(name, quarkversion, scversion)).throw };
			winner["version"] = quarkversion;
		}{ // if quarkversion is nil, find latest compatible version, return that
			winner = candidates[candidates.keys.asArray.sort.last];
			winner["version"] = candidates.keys.asArray.sort.last;
		}
		^winner
	}

	*fromQuarks1 {
		// For each old-style quark that is installed, install it the new way
		Quarks.installed.do{|q|
			"Converting quark '%'".format(q.name).postln;
			this.install(q.name, Main.version, q.version);
			// deactivate oldquarks install (to prevent classnameclashogeddon)
			LanguageConfig.removeIncludePath(Platform.userExtensionDir +/+ Quarks.local.name +/+ q.path);
			LanguageConfig.store;
		};
	}
}
