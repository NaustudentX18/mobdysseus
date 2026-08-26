#!/usr/bin/env python3
import cairosvg, os
D = os.path.dirname(os.path.abspath(__file__))
BG="#0d1b12"; SURF="#13281a"; SURF2="#173320"; PRIM="#2fae6f"; PRIMD="#1f7a4d"; GOLD="#e9b35c"; TX="#e9f4ec"; TX2="#9fc6ad"
def render(name, body):
    H=880
    svg=('<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 420 880" width="840" height="1760">'
         '<rect x="0" y="0" width="420" height="880" rx="54" fill="#171717"/>'
         '<rect x="30" y="40" width="360" height="800" rx="20" fill="'+BG+'"/>'+body+'</svg>')
    cairosvg.svg2png(bytestring=svg.encode(), write_to=os.path.join(D,name+'.png'), output_width=840, output_height=1760)
    print('wrote',name+'.png')
def bar(title):
    s='<text x="58" y="66" font-family="sans-serif" font-size="20" fill="#e9f4ec">9:41</text>'
    s+='<text x="300" y="62" font-family="sans-serif" font-size="18" fill="#e9f4ec">\u25cf \u25c9 \u25cf</text>'
    s+='<text x="58" y="118" font-family="sans-serif" font-size="32" font-weight="700" fill="#e9f4ec">'+title+'</text>'
    return s
def card(x,y,w,h): return '<rect x="%d" y="%d" width="%d" height="%d" rx="14" fill="#13281a"/>'%(x,y,w,h)
def fab():
    return ('<circle cx="334" cy="790" r="30" fill="#2fae6f"/>'
            '<path d="M334 760 v36 M316 790 h36" stroke="#06130b" stroke-width="7" stroke-linecap="round"/>')
def dots(x,y,n=3):
    s=''
    for i in range(n): s+='<circle cx="%d" cy="%d" r="4" fill="#e9b35c"/>'%(x+i*16,y)
    return s
def chat():
    b=bar('Chat')
    b+='<rect x="46" y="148" width="252" height="78" rx="16" fill="#13281a"/>'
    b+='<text x="66" y="176" font-family="sans-serif" font-size="18" fill="#e9f4ec">Hello! I am Mobdysseus,</text>'
    b+='<text x="66" y="198" font-family="sans-serif" font-size="18" fill="#e9f4ec">running fully on-device.</text>'
    b+='<rect x="180" y="236" width="236" height="48" rx="10" fill="#1f7a4d"/>'
    b+='<text x="198" y="266" font-family="sans-serif" font-size="17" fill="#eafff2">Plan my week, please</text>'
    b+='<rect x="46" y="316" width="374" height="170" rx="10" fill="#13281a"/>'
    b+='<text x="66" y="346" font-family="sans-serif" font-size="17" fill="#e9f4ec">Here is your weekly plan:</text>'
    for i,(w,go) in enumerate([(240,0),(270,0),(180,1),(220,0),(140,0)]):
        y=364+i*16
        col="#e9b35c" if go else "#9fc6ad"
        b+='<rect x="66" y="%d" width="%d" height="8" rx="4" fill="%s" opacity="0.5"/>'%(y,w,col)
    b+='<rect x="46" y="744" width="374" height="46" rx="23" fill="#173320"/>'
    b+='<rect x="64" y="762" width="180" height="10" rx="5" fill="#9fc6ad" opacity="0.4"/>'
    b+='<circle cx="360" cy="767" r="24" fill="#2fae6f"/><path d="M360 754 v26 l-10 -14 z" fill="#fff"/>'
    return b
render('screenshot-chat',chat())
def calendar():
    b=bar('Calendar')
    names=['Mon','Tue','Wed','Thu','Fri','Sat','Sun']
    for i,nm in enumerate(names):
        b+='<text x="%d" y="150" font-family="sans-serif" font-size="14" fill="#9fc6ad">%s</text>'%(48+i*52,nm)
    rows=[[1,2,3,4,5,6,7],[8,9,10,11,12,13,14],[15,16,17,18,19,20,21],[22,23,24,25,26,27,28],[29,30,31,0,0,0,0]]
    for r,row in enumerate(rows):
        y=180+r*64
        for c,d in enumerate(row):
            if d==0: continue
            x=56+c*52
            hl = d in (5,8,17,24,29)
            if hl:
                b+='<circle cx="%d" cy="%d" r="22" fill="#2fae6f" opacity="0.85"/>'%(x+14,y-6)
                b+='<text x="%d" y="%d" font-family="sans-serif" font-size="20" font-weight="700" fill="#eafff2">%d</text>'%(x+8,y+18,d)
            else:
                b+='<text x="%d" y="%d" font-family="sans-serif" font-size="18" fill="#e9f4ec">%d</text>'%(x+10,y+18,d)
            if d in (3,11,21,26): b+=dots(x+14,y+32)
    b+=card(46,506,374,200)
    b+='<text x="66" y="538" font-family="sans-serif" font-size="18" font-weight="700" fill="#e9f4ec">Tuesday, 26 Aug</text>'
    b+='<rect x="66" y="554" width="120" height="8" rx="4" fill="#2fae6f"/>'
    b+='<text x="66" y="592" font-family="sans-serif" font-size="16" fill="#9fc6ad">09:00 \u2013 10:00 \u00b7 Weekly plan</text>'
    b+='<rect x="66" y="610" width="200" height="8" rx="4" fill="#9fc6ad" opacity="0.4"/>'
    b+='<text x="66" y="646" font-family="sans-serif" font-size="16" fill="#9fc6ad">14:30 \u2013 15:00 \u00b7 Standup</text>'
    b+='<rect x="66" y="662" width="180" height="8" rx="4" fill="#9fc6ad" opacity="0.4"/>'
    b+=dots(66,706,3)
    return b+fab()
render('screenshot-calendar',calendar())
def cookbook():
    b=bar('Cookbook')
    b+='<rect x="46" y="140" width="374" height="58" rx="12" fill="#13281a"/>'
    b+='<text x="66" y="166" font-family="sans-serif" font-size="16" font-weight="700" fill="#e9f4ec">Galaxy S25 \u00b7 Snapdragon 8 Elite</text>'
    b+='<text x="66" y="188" font-family="sans-serif" font-size="14" fill="#9fc6ad">12 GB RAM \u00b7 Adreno 830</text>'
    models=[('Qwen 2.5 3B Q4',92),('Llama 3.2 3B Q4',85),('Qwen 2.5 1.5B Q4',74),('qwen3 0.6B Q8',62)]
    for i,(m,score) in enumerate(models):
        y=216+i*104
        b+=card(46,y,374,96)
        b+='<text x="66" y="%d" font-family="sans-serif" font-size="17" font-weight="700" fill="#e9f4ec">%s</text>'%(y+28,m)
        b+='<text x="66" y="%d" font-family="sans-serif" font-size="14" fill="#9fc6ad">%d tok/s \u00b7 fit</text>'%(y+48,score)
        b+='<rect x="66" y="%d" width="280" height="9" rx="4.5" fill="#173320"/>'%(y+58)
        b+='<rect x="66" y="%d" width="%d" height="9" rx="4.5" fill="#2fae6f"/>'%(y+58,int(280*score/100))
        b+='<text x="356" y="%d" font-family="sans-serif" font-size="16" font-weight="700" fill="#e9b35c">%d%%</text>'%(y+26,score)
    return b
def mcp():
    b=bar('MCP Tools')
    b+='<text x="58" y="146" font-family="sans-serif" font-size="14" fill="#9fc6ad">SERVERS</text>'
    b+=card(46,160,374,92)
    b+='<circle cx="80" cy="206" r="20" fill="#2fae6f"/><text x="80" y="214" font-family="sans-serif" font-size="18" font-weight="700" fill="#fff">P</text>'
    b+='<text x="112" y="200" font-family="sans-serif" font-size="17" font-weight="700" fill="#e9f4ec">Pi AiServer</text>'
    b+='<text x="112" y="222" font-family="sans-serif" font-size="14" fill="#9fc6ad">192.168.4.44 \u00b7 21 tools</text>'
    b+='<text x="66" y="286" font-family="sans-serif" font-size="14" fill="#9fc6ad">TOOLS</text>'
    tools=[('read_sensor','Read temperature','Run'),('weather','Forecast','Run'),('search','Web search','Run')]
    for i,(name,desc,_) in enumerate(tools):
        y=300+i*92
        b+=card(66,y,334,78)
        b+='<text x="84" y="%d" font-family="sans-serif" font-size="16" font-weight="700" fill="#e9f4ec">%s</text>'%(y+28,name)
        b+='<text x="84" y="%d" font-family="sans-serif" font-size="14" fill="#9fc6ad">%s</text>'%(y+48,desc)
        b+='<rect x="330" y="%d" width="52" height="30" rx="15" fill="#2fae6f"/>'%(y+24)
        b+='<text x="342" y="%d" font-family="sans-serif" font-size="14" font-weight="700" fill="#eafff2">Run</text>'%(y+44)
    return b+fab()
render('screenshot-cookbook',cookbook())
render('screenshot-mcp',mcp())
def notes():
    b=bar('Notes')
    notes=[('Grocery list','Milk, eggs, bread...',3),('Ideas','S25 on-device AI',0),('Meeting notes','Q3 planning sync...',5)]
    for i,(t,s,mark) in enumerate(notes):
        y=150+i*108
        b+=card(46,y,374,96)
        b+='<circle cx="80" cy="%d" r="14" fill="%s"/>'%(y+28, "#e9b35c" if i==0 else "#2fae6f")
        b+='<text x="82" y="%d" font-family="sans-serif" font-size="16" font-weight="700" fill="#0d1b12">%d</text>'%(y+34,i+1)
        b+='<text x="104" y="%d" font-family="sans-serif" font-size="18" font-weight="700" fill="#e9f4ec">%s</text>'%(y+30,t)
        b+='<text x="104" y="%d" font-family="sans-serif" font-size="14" fill="#9fc6ad">%s</text>'%(y+52,s)
        if mark==3: dots(104,y+74)
        elif mark==5: b+='<text x="104" y="%d" font-family="sans-serif" font-size="13" fill="#e9b35c">updated 5m ago</text>'%(y+70)
    return b+fab()
def settings():
    b=bar('Settings')
    b+='<text x="58" y="148" font-family="sans-serif" font-size="15" font-weight="700" fill="#e9f4ec">Model source</text>'
    b+='<rect x="48" y="160" width="180" height="40" rx="20" fill="#2fae6f"/>'
    b+='<text x="66" y="186" font-family="sans-serif" font-size="15" fill="#eafff2">On-device</text>'
    b+='<rect x="236" y="160" width="160" height="40" rx="20" fill="#13281a"/>'
    b+='<text x="254" y="186" font-family="sans-serif" font-size="15" fill="#9fc6ad">Cloud API</text>'
    b+='<text x="58" y="226" font-family="sans-serif" font-size="15" font-weight="600" fill="#e9f4ec">Model (GGUF)</text>'
    models=['Qwen 2.5 3B Q4','Qwen 2.5 1.5B Q4','Llama 3.2 3B Q4']
    for i,m in enumerate(models):
        y=240+i*46
        b+='<rect x="58" y="%d" width="330" height="40" rx="10" fill="%s"/>'%(y,"#2fae6f" if i==0 else "#13281a")
        b+='<text x="76" y="%d" font-family="sans-serif" font-size="15" fill="%s">%s</text>'%(y+26,"#eafff2" if i==0 else "#e9f4ec",m)
        if i==0: b+='<path d="M340 %d l8 8 l16 -16" stroke="#eafff2" stroke-width="4" fill="none"/>'%(y+12)
    b+=card(48,384,374,120)
    b+='<text x="66" y="414" font-family="sans-serif" font-size="15" font-weight="700" fill="#e9f4ec">About</text>'
    b+='<text x="66" y="438" font-family="sans-serif" font-size="13" fill="#9fc6ad">Mobdysseus v0.7.1</text>'
    b+='<text x="66" y="460" font-family="sans-serif" font-size="13" fill="#9fc6ad">On-device AI workspace for Galaxy S25.</text>'
    b+='<text x="66" y="480" font-family="sans-serif" font-size="13" fill="#e9b35c">AGPL-3.0 \u00b7 community build</text>'
    return b
render('screenshot-notes',notes())
render('screenshot-settings',settings())
