Set-Location 'D:\ATG\app\backend\creditos'
$files = Get-ChildItem 'target\*.jar' | Where-Object { $_.Name -notmatch 'original' }
foreach ($f in $files) {
    Copy-Item $f.FullName 'app.jar' -Force
    Write-Host ('Copied ' + $f.Name + ' to app.jar')
}
if (Test-Path 'app.jar') {
    $sz = (Get-Item 'app.jar').Length
    Write-Host ('app.jar exists, size: ' + $sz + ' bytes')
} else {
    Write-Host 'app.jar NOT found'
}
