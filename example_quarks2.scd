
// Re-grab the metadata for available quarks
Quarks2.refresh

// This is for dev purposes: load the quark info from disk into 
a = Quarks2.getQuarksInfo
a.keys
a["cruciallib"].keysValuesDo{|k,v| "ENTRY ".post; k.postln; v.postln;}
a["cruciallib"]["version"]

// Not implemented yet - this downloads the requested version
Quarks2.fetchQuark("cruciallib", "3.4", "0.2")
// Not implemented yet
Quarks2.installQuark("cruciallib", "3.4", "0.2")



