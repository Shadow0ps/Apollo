Set fso = CreateObject("Scripting.FileSystemObject")
const ReadOnly = 1
If  ( (WScript.Arguments.Count = 3) AND (fso.FolderExists(WScript.Arguments(0))) AND (fso.FolderExists( WScript.Arguments(1) )) AND (("false" = LCase(WScript.Arguments(2)) ) Or ( "true" = LCase(WScript.Arguments(2)) ))) Then
	WScript.Echo "Starting Platform Dependent Updater"
	WScript.Echo "Waiting 3 sec"
	WScript.Sleep 3000
	CURRENT_CONF_FILE = WScript.Arguments(0) & "/conf/apl.properties"
	UPDATE_CONF_FILE = WScript.Arguments(1) & "/conf/apl.properties"
	If (fso.FileExists(CURRENT_CONF_FILE)) Then
		WScript.Echo "Copy config file"
		fso.CopyFile CURRENT_CONF_FILE, UPDATE_CONF_FILE, True
    Else
		WScript.Echo "Config file:" & CURRENT_CONF_FILE & " does not exist!"
	End If
    WScript.Echo "Copy update files"
    Set fso = CreateObject("Scripting.FileSystemObject")
	Set objFolder = fso.GetFolder(WScript.Arguments(1))

	Wscript.Echo objFolder.Path

	Set colFiles = objFolder.Files
	Wscript.Echo "Copy root files"
	For Each objFile in colFiles
		targetFilePath = Wscript.Arguments(0) & "\" & objFile.Name
		isReadonly = MakeReadWrite(targetFilePath)
		fso.CopyFile objFile.Path, Wscript.Arguments(0) & "\", True
		if (isReadonly) then
			MakeReadonly(targetFilePath)
		End If
    Next
	Wscript.Echo "Root files were copied. Copy subfolders..."


	CopySubfolders fso.GetFolder(objFolder)

	Wscript.Echo "Subfolders were copied"
	Set objShell = Wscript.CreateObject("WScript.Shell")
	if  ("true" = LCase(WScript.Arguments(2))) Then
        WScript.Echo "Start desktop application"
        objShell.Run WScript.Arguments(0) & "\start.vbs"
    else
        WScript.Echo "Start command line application"
        objShell.Run WScript.Arguments(0) & "\start.vbs"
    End If
	WScript.Echo "Exit"
Else
	WScript.Echo "Invalid input parameters:" & WScript.Arguments(0) & " " & WScript.Arguments(1) & " " & WScript.Arguments(2)
End If
Sub CopySubFolders(Folder)
    For Each Subfolder in Folder.SubFolders
		targetFolderPath = Replace(SubFolder.Path, WScript.Arguments(1), WScript.Arguments(0))
		if (Not fso.FolderExists(targetFolderPath)) then
			fso.CreateFolder targetFolderPath
		End If

        Set objFolder = fso.GetFolder(Subfolder.Path)

        Set colFiles = objFolder.Files

        For Each objFile in colFiles
			targetFilePath = targetFolderPath & "\" & objFile.Name
			isReadonly = MakeReadWrite(targetFilePath)
			fso.CopyFile objFile.Path, targetFolderPath & "\", True
			if (isReadonly) then
				MakeReadonly(targetFilePath)
			End If
        Next

        CopySubFolders Subfolder

    Next

End Sub

Function MakeReadWrite(File)
    		MakeReadWrite = false
			if (fso.FileExists(File)) then
				Set f = fso.GetFile(File)
				If f.Attributes AND ReadOnly Then
					f.Attributes = f.Attributes XOR ReadOnly
					MakeReadWrite = true
				End If
			End If
End Function
Function MakeReadonly(File)
    		MakeReadonly = false
			if (fso.FileExists(File)) then
				Set f = fso.GetFile(File)
				If Not(f.Attributes AND ReadOnly) Then
					f.Attributes = f.Attributes XOR ReadOnly
					MakeReadonly = true
				End If
			End If
End Function