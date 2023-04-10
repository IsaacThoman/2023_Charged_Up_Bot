const canvas = document.getElementById("gameCanvas");
const ctx = canvas.getContext("2d");

let playerX = 0;
let playerY = 0;
let playerAngle = 0;
const pixelsToMeters = 50;
const maxSpeedMetersPerSecond = 3.5;
const arrowsRadsPerSecond = 3;
function periodic(){

    fillSky();
    drawArrows();
    processInputs();

    requestAnimationFrame(periodic);
}
requestAnimationFrame(periodic);

function drawArrows(){

    let actualX =  NetworkTables.getValue("/SmartDashboard/webdriver_actualX",0);
    let actualY =  NetworkTables.getValue("/SmartDashboard/webdriver_actualY",0);
    let actualAngle =  NetworkTables.getValue("/SmartDashboard/webdriver_actualAngle",0);

    drawArrow(actualX*pixelsToMeters+320/2,-actualY*pixelsToMeters+200/2,actualAngle,"#00ff00"); //desired
    drawArrow(playerX*pixelsToMeters+320/2,-playerY*pixelsToMeters+200/2,playerAngle,"#ff0000"); //desired


}
function fillSky(){
    ctx.fillStyle = "#2a2a2a";
    ctx.beginPath();
    ctx.rect(0,0,1000,screen.height);
    ctx.fill();
    ctx.closePath();
}
let keys; resetKeys();
function resetKeys(){ keys = {'arrowup':false,'arrowdown':false,'arrowleft':false,'arrowright':false, 'w':false, 'a':false,'s':false,'d':false,'shift':false,'control':false,'u':false,'h':false,'j':false,'k':false,'space':false,'i':false} }


document.addEventListener("keydown", keyDownHandler, false);
document.addEventListener("keyup", keyUpHandler, false);
function keyDownHandler(e){
    keys[e.key.toLowerCase()] = true;
}
function keyUpHandler(e){
    keys[e.key.toLowerCase()] = false;
}

function drawArrow(x,y,angle,color){
    angle = -angle;
  //  angle-=Math.PI/2;
    ctx.strokeStyle = color;
    ctx.beginPath();
    ctx.moveTo(x-Math.cos(angle)*10,y-Math.sin(angle)*10);
    let tipXY = [x+Math.cos(angle)*10,y+Math.sin(angle)*10];
    ctx.lineTo(tipXY[0],tipXY[1]);
    let tipAngle = Math.PI/2*1.5;
    ctx.lineTo(tipXY[0]+Math.cos(angle+tipAngle)*6,tipXY[1]+Math.sin(angle+tipAngle)*6);
    ctx.moveTo(tipXY[0],tipXY[1]);
    ctx.lineTo(tipXY[0]+Math.cos(angle-tipAngle)*6,tipXY[1]+Math.sin(angle-tipAngle)*6);
    ctx.stroke();
    ctx.closePath();
}

function processInputs(){
    let xSpeed = 0;
    let ySpeed = 0;
    let angleChange = 0;
    let keyHeld = keys['w'] || keys['a'] || keys['s'] || keys['d'];
    if(keys['w']){xSpeed += 1;}
    if(keys['s']){xSpeed -= 1;}
    if(keys['a']){ySpeed += 1;}
    if(keys['d']){ySpeed -= 1;}
    if(keys['arrowleft']){angleChange += arrowsRadsPerSecond;}
    if(keys['arrowright']){angleChange -= arrowsRadsPerSecond;}

    let angle = Math.atan2(ySpeed,xSpeed);
    let speed = maxSpeedMetersPerSecond;
    let affSpeed = 0.35;
    let affRotationSpeed = 3.5;
    if(!keyHeld){speed = 0; affSpeed = 0;}
    playerX += Math.cos(angle+playerAngle)*speed*((Date.now()-lastInputTime)/1000);
    playerY += Math.sin(angle+playerAngle)*speed*((Date.now()-lastInputTime)/1000);
    playerAngle += angleChange*((Date.now()-lastInputTime)/1000);

    playerAngle %= Math.PI*2;

    NetworkTables.setValue("/SmartDashboard/webdriver_desiredX",playerX);
    NetworkTables.setValue("/SmartDashboard/webdriver_desiredY",playerY);
    NetworkTables.setValue("/SmartDashboard/webdriver_desiredAngle",playerAngle);

    NetworkTables.setValue("/SmartDashboard/webdriver_affX",Math.cos(angle)*affSpeed);
    NetworkTables.setValue("/SmartDashboard/webdriver_affY",Math.sin(angle)*affSpeed);
    NetworkTables.setValue("/SmartDashboard/webdriver_affRotation",angleDiffRad(lastPlayerAngle,playerAngle)*affRotationSpeed);
    lastPlayerAngle = playerAngle;

    lastInputTime = Date.now();

}

function angleDiffRad(ang1, ang2) {
    const pi = Math.PI;
    const twoPi = 2 * pi;
    let diff = (ang2 - ang1) % twoPi;

    if (Math.abs(diff) > pi) {
        diff = diff > 0 ? diff - twoPi : diff + twoPi;
    }
    return diff;
}

let lastInputTime = 0;
let lastPlayerAngle = 0;

// mouse locking from https://developer.mozilla.org/en-US/docs/Web/API/Pointer_Lock_API
//stolen directly from suburban wasteland :)
canvas.requestPointerLock = canvas.requestPointerLock;  //mouse lock stuff
document.exitPointerLock = document.exitPointerLock;
canvas.onclick = function() {
    canvas.requestPointerLock();
    cKeyPressed();
}
document.addEventListener('pointerlockchange', toggleUseMouse, false);
let usingMouse = false;
function toggleUseMouse(){
    usingMouse = (document.pointerLockElement === canvas);
    if(usingMouse)
        document.addEventListener("mousemove", mouseUpdate, false);
    else
        document.removeEventListener("mousemove", mouseUpdate, false);
}
function mouseUpdate(e){
    playerAngle-= e.movementX/250;
}