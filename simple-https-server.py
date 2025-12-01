#!/usr/bin/env python3
"""
Simple HTTPS server for AllSenses Demo
Enables microphone access for enhanced emergency detection
"""

import http.server
import ssl
import os
import sys
from pathlib import Path

def main():
    # Set port
    port = 8443
    
    # Check if we're in the right directory
    current_dir = Path.cwd()
    
    # Look for enhanced-emergency-monitor.html
    demo_file = None
    search_paths = [
        current_dir / "enhanced-emergency-monitor.html",
        current_dir / "frontend" / "enhanced-emergency-monitor.html",
        current_dir / "hackathon-jury-demo.html",
        current_dir / "frontend" / "hackathon-jury-demo.html"
    ]
    
    for path in search_paths:
        if path.exists():
            demo_file = path
            os.chdir(path.parent)
            break
    
    if not demo_file:
        print("❌ Demo files not found. Please run from the project root directory.")
        sys.exit(1)
    
    print(f"✅ Found demo file: {demo_file}")
    print(f"✅ Serving from: {Path.cwd()}")
    
    # Create HTTPS server
    server_address = ('localhost', port)
    httpd = http.server.HTTPServer(server_address, http.server.SimpleHTTPRequestHandler)
    
    # Create SSL context with self-signed certificate
    context = ssl.create_default_context(ssl.Purpose.CLIENT_AUTH)
    context.check_hostname = False
    context.verify_mode = ssl.CERT_NONE
    
    # Create a simple self-signed certificate in memory
    import tempfile
    import subprocess
    
    print("🔐 Creating self-signed certificate for localhost...")
    
    try:
        with tempfile.TemporaryDirectory() as temp_dir:
            cert_file = os.path.join(temp_dir, "cert.pem")
            key_file = os.path.join(temp_dir, "key.pem")
            
            # Generate self-signed certificate
            cmd = [
                "openssl", "req", "-x509", "-newkey", "rsa:2048", 
                "-keyout", key_file, "-out", cert_file, "-days", "1", 
                "-nodes", "-subj", "/CN=localhost"
            ]
            
            result = subprocess.run(cmd, capture_output=True, text=True)
            if result.returncode != 0:
                raise Exception(f"OpenSSL failed: {result.stderr}")
            
            # Load the certificate
            context.load_cert_chain(cert_file, key_file)
            
    except Exception as e:
        print(f"⚠️  Could not create SSL certificate: {e}")
        print("🔧 Falling back to HTTP server on port 8080...")
        
        # Fallback to HTTP
        httpd = http.server.HTTPServer(('localhost', 8080), http.server.SimpleHTTPRequestHandler)
        print("🚀 AllSenses HTTP Demo Server Starting...")
        print("📱 Access demo at: http://localhost:8080/enhanced-emergency-monitor.html")
        print("⚠️  Note: Microphone access may be blocked on HTTP")
        
        try:
            httpd.serve_forever()
        except KeyboardInterrupt:
            print("\n🛑 Server stopped")
            httpd.shutdown()
        return
    
    # Wrap socket with SSL
    httpd.socket = context.wrap_socket(httpd.socket, server_side=True)
    
    print("🚀 AllSenses HTTPS Demo Server Starting...")
    print(f"📱 Access demo at: https://localhost:{port}/enhanced-emergency-monitor.html")
    print(f"🎯 Jury demo at: https://localhost:{port}/hackathon-jury-demo.html")
    print(f"🔧 Complete demo at: https://localhost:{port}/mvp1-complete-demo.html")
    print("🎤 Microphone access enabled with HTTPS!")
    print("⚠️  Accept the self-signed certificate warning in your browser")
    print("   (Click 'Advanced' → 'Proceed to localhost (unsafe)')")
    print("🛑 Press Ctrl+C to stop the server")
    
    try:
        httpd.serve_forever()
    except KeyboardInterrupt:
        print("\n🛑 Server stopped")
        httpd.shutdown()

if __name__ == "__main__":
    main()