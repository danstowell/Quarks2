// Re-grab the metadata for available quarks
Quarks2.refresh

// This lists the available ones
Quarks2.getQuarksInfo.keys

Quarks2.install("cruciallib", 1.0);  // Install a specific quark version

Quarks2.install("cruciallib");    // short ver not yet working (need to be clever with defaults)

Quarks2.installed;   // get a list of what's installed

Quarks2.uninstall("cruciallib");




// This is for dev purposes: inspect quark info
a = Quarks2.getQuarksInfo
a.keys
a["cruciallib"].keysValuesDo{|k,v| "ENTRY ".post; k.postln; v.postln;}
a["cruciallib"]["version"]

