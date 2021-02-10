# Print datagram string from UPD port
# Waits forever for a packet; loops forever looking for more packets
# Powershell script
$port = 5800
$listener = New-Object System.Net.IPEndPoint ([system.net.ipaddress]::any,$port)
$udpclient = New-Object System.Net.Sockets.UdpClient ($listener)
Write-Host "Starting Loop"
while($true)
{
$content=$udpclient.Receive([ref]$listener)
echo ([text.encoding]::ascii.getstring($content))
}
$client.close()