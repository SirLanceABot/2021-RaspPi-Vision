# Print datagram string from UPD port
# Waits forever for a packet; loops forever looking for more packets
# Powershell script
# To limit output, for example, only the Turret:
# If (([text.encoding]::ascii.getstring($content)).substring(0, 6) -eq "Turret") {echo ([text.encoding]::ascii.getstring($content))}
# If the Windows laptop network won't pass these packets because of the firewall, you might change the network profile from Public to Private
# In Powershell running as administrator run:
# Get-NetConnectionProfile # to see the active networks
# Set-NetConnectionProfile -Name "Unidentified network" -NetworkCategory Private # change the wired network on a laptop for example
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
