package s0591904;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.geom.Path2D;
import java.text.AttributedCharacterIterator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import lenz.htw.ai4g.ai.AI;
import lenz.htw.ai4g.ai.DivingAction;
import lenz.htw.ai4g.ai.Info;
import lenz.htw.ai4g.ai.PlayerAction;


public class MyAi extends AI {

    private Point[] pearls;
    private Path2D[] obstacles;
    private List<Point> checkedTargets = new ArrayList<Point>();
    private List<Point> allTargets = new ArrayList<Point>();
    float[] target;
    private float[] avoidTargetCenter;
    private float[] avoidTargetRight;
    private float[] avoidTargetLeft;
    float[] avoidDirection;
    boolean avoiding = false;
    float[] predictionPointCenter;
    private float[] predictionPointLeft;
    private float[] predictionPointRight;

    int currentTargetIndex = 0;
    int currentScore = 0;
    private final float maxAngularVelocity = (float) Math.toRadians(10f);
    private final float maxAngularAcceleration = (float) Math.toRadians(5f);



    public MyAi(Info info) {
        super(info);
        enlistForTournament(591904);
        pearls = info.getScene().getPearl();
        obstacles = info.getScene().getObstacles();
        //sortiert Pearls von rechts nach links
        Arrays.sort(pearls, (a, b) -> {
            double ax = a.x;
            double bx = b.x;
            return Double.compare(bx, ax);
        });
        allTargets.addAll(Arrays.asList(pearls));
        target = new float[]{
                pearls[currentTargetIndex].x,
                pearls[currentTargetIndex].y};
    }

    @Override
    public String getName() {
        return "Lorenz";
    }

    @Override
    public Color getPrimaryColor() {
        return Color.BLUE;
    }

    @Override
    public Color getSecondaryColor() {
        return new Color(0, 255, 0);
    }

    @Override
    public PlayerAction update() {
        float power = 0;
        float angularAcceleration = 0;
        Path2D obstacle = null;
        predictionPointCenter = PredictionAhead(0, 25f); //center
        predictionPointLeft = PredictionAhead((float) Math.toRadians(30f), 25f); //left
        predictionPointRight = PredictionAhead((float) Math.toRadians(-30f), 25f); //right

        for (Path2D obs : obstacles) {
            if (!obs.contains(predictionPointCenter[0], predictionPointCenter[1]) && !obs.contains(predictionPointRight[0], predictionPointRight[1]) && !obs.contains(predictionPointLeft[0], predictionPointLeft[1])) {
                avoiding = false;
            } else {
                avoiding = true;
                obstacle = obs;
                break;
            }

        }
        if(!avoiding){
            if (allTargets.isEmpty())
                return new DivingAction(0, 0);
            if (info.getScore() > currentScore) {
                if(target[0] < info.getX()+10 && target[0] > info.getX()-10)
                {
                    allTargets.remove(allTargets.get(currentTargetIndex));
                    target = new float[]{
                            allTargets.get(currentTargetIndex).x,
                            allTargets.get(currentTargetIndex).y};;
                }
                else
                {
                    for(Point p : allTargets)
                    {

                        if(p.x < info.getX()+10 && p.x > info.getX()-10)
                        {
                            System.out.println("p:"+p.x);
                            System.out.println("pos:"+info.getX());
                            allTargets.remove(p);
                            break;
                        }
                    }
                }
                currentScore++;

            }

            power = 10f;//Arrive(target, 1.0f, 5.0f, 0.1f);
            angularAcceleration = Align(target, 3f);
        }
        else
        {
            if (obstacle.contains(predictionPointCenter[0], predictionPointCenter[1])) {
                avoidTargetCenter = AvoidObstacles(predictionPointCenter);
                power = 10f;//(float) Math.sqrt(avoidanceForce[0]*avoidanceForce[0] + avoidanceForce[1]*avoidanceForce[1]);
                angularAcceleration = Align(avoidTargetCenter, 3f);
            }
            if (obstacle.contains(predictionPointRight[0], predictionPointRight[1])) {
                avoidTargetRight = AvoidObstacles(predictionPointRight);
                power = 10f;//(float) Math.sqrt(avoidanceForce[0]*avoidanceForce[0] + avoidanceForce[1]*avoidanceForce[1]);
                angularAcceleration = Align(avoidTargetRight, 3f);
            }
            if (obstacle.contains(predictionPointLeft[0], predictionPointLeft[1])) {
                avoidTargetLeft = AvoidObstacles(predictionPointLeft);
                power = 10f;//(float) Math.sqrt(avoidanceForce[0]*avoidanceForce[0] + avoidanceForce[1]*avoidanceForce[1]);
                angularAcceleration = Align(avoidTargetLeft, 3f);
            }
        }



        return new DivingAction(power, angularAcceleration);
    }

    private float distanceTo(float[] target) {
        float dx = target[0] - info.getX();
        float dy = target[1] - info.getY();
        return (float) Math.sqrt(dx*dx + dy*dy);
    }

    private float[] PredictionAhead(float degree, float distance)
    {
        //predict where I am going
        float orientation = info.getOrientation() + degree;
        float speed = info.getVelocity() * distance;
        float[] ahead = new float[]{
                info.getX() + (float)Math.cos(orientation) * speed,
                info.getY() - (float)Math.sin(orientation) * speed
        };
        return ahead;
    }

    private float[] AvoidObstacles(float[] predictionPoint) {

        avoidDirection = new float[]{
                info.getX() - predictionPoint[0],
                info.getY() - predictionPoint[1]
        };
        float multiplier = 50f; // Wie weit soll der Schwimmer ausweichen
        float[] counterTarget = new float[]{
                info.getX() + avoidDirection[0] * multiplier,
                info.getY() + avoidDirection[1] * multiplier
        };

        return counterTarget;
    }


    private float Align(float[] targetPos, float timeToTarget) {
        float alignToleranz = (float) Math.toRadians(1.5f);
        float slowDownAngle = (float) Math.toRadians(30f);

        float[] direction = new float[]{targetPos[0] - info.getX(), targetPos[1] - info.getY()}; //richtung zum Target
        float targetAngle = (float) Math.atan2(-direction[1], direction[0]); //Angle zum Target

        // kleinster Winkel zwischen 2 Orientierungen
        float rotation = normalizeAngle(targetAngle - info.getOrientation()); //richtung
        float rotationSize = Math.abs(rotation); //winkelbetrag = wie viel noch

        if (rotationSize < alignToleranz) {
            return 0; //arrived
        }

        // Ziel-Drehgeschwindigkeit berechnen
        float targetAngularVelocity;
        if (rotationSize <= slowDownAngle) {
            targetAngularVelocity = rotationSize * maxAngularVelocity / slowDownAngle;
        } else {
            targetAngularVelocity = maxAngularVelocity;
        }

        // Richtung berücksichtigen
        targetAngularVelocity *= Math.signum(rotation);

        // Wunsch-Drehbeschleunigung
        float angularAcceleration = (targetAngularVelocity - info.getAngularVelocity()) / timeToTarget;

        if (Math.abs(angularAcceleration) > maxAngularAcceleration) {
            angularAcceleration = maxAngularAcceleration * Math.signum(angularAcceleration);
        } //Begrenzung

        return angularAcceleration;

    }

    // Gibt einen Winkel zurück im Bereich [-π, +π]
    float normalizeAngle(float angle) {
        while (angle > Math.PI) angle -= 2 * Math.PI;
        while (angle < -Math.PI) angle += 2 * Math.PI;
        return angle;
    }

    private float Arrive (float[] target, float targetRadius, float slowDownRadius, float timeToTarget )
    {
        //Richtung & Abstand berechnen
        float[] direction = new float[] {target[0]-info.getX(),target[1]-info.getY()};
        float distance = (float) Math.sqrt(Math.pow(direction[0],2) + Math.pow(direction[1],2));
        // Ziel erreicht → keine Bewegung
        if(distance < targetRadius)
            return 0;
        //Wunschgeschwindigkeit bestimmen
        float targetSpeed;
        if(distance < slowDownRadius)
        {
            targetSpeed = info.getMaxVelocity() * (distance/slowDownRadius);
        }
        else
        {
            targetSpeed = info.getMaxVelocity();
        }

        //Wunschgeschwindigkeit = Richtung * Zielgeschwindigkeit
        float[] desiredVelocity = new float[] {direction[0]/distance * targetSpeed, direction[1]/distance * targetSpeed};
        float velocityX = (float) (Math.cos(info.getOrientation()) * info.getVelocity());
        float velocityY = (float) (Math.sin(info.getOrientation()) * info.getVelocity());

        //Beschleunigung berechnen
        float[] desiredAcceleration = new float[] {
                desiredVelocity[0]-velocityX * (1/timeToTarget),
                desiredVelocity[1]-velocityY * (1/timeToTarget)
        };
        float accMagnitude = (float) Math.sqrt(Math.pow(desiredAcceleration[0],2) + Math.pow(desiredAcceleration[1],2));
        if (accMagnitude > info.getMaxAcceleration()) {
            desiredAcceleration[0] = desiredAcceleration[0] / accMagnitude * info.getMaxAcceleration();
            desiredAcceleration[1] = desiredAcceleration[1] / accMagnitude * info.getMaxAcceleration();
            accMagnitude = info.getMaxAcceleration(); // reset it
        }

        float[] facing = new float[]{
                (float) Math.cos(info.getOrientation()),
                (float) Math.sin(info.getOrientation())};

        // Direction to target (normalized)
        float[] desiredDir = new float[]{direction[0] / distance, direction[1] / distance};
        float dot = facing[0] * desiredDir[0] + facing[1] * desiredDir[1];
        dot = Math.max(-1, Math.min(1, dot));
        float angleBetween = (float) Math.acos(dot);

        float angleTolerance = (float) Math.toRadians(22);
        float alignment = (angleBetween < angleTolerance) ? 1f : Math.max(0f, dot);

        // Final power = acceleration * alignment factor
        float power = accMagnitude * alignment;
        return power;
    }


    @Override
    public void drawDebugStuff(Graphics2D gfx) {
        for (Point p : allTargets) {
            gfx.setColor(Color.BLACK);
            gfx.drawString(String.valueOf(p.x), p.x, p.y);

        }
        gfx.setColor(Color.RED);
        gfx.drawLine((int) info.getX(), (int) info.getY(), (int) target[0], (int) target[1]);

        if(target[0] < info.getX()+10 && target[0] > info.getX()-10)
        {
            gfx.drawString(Arrays.toString(target), target[0], target[1]);
        }

        if(predictionPointCenter != null)
        {
            gfx.setColor(Color.MAGENTA);
            gfx.drawOval((int) predictionPointCenter[0], (int) predictionPointCenter[1],1,1);

//            if(avoidTargetCenter != null){
//                gfx.setColor(Color.YELLOW);
//                gfx.drawLine((int) predictionPointCenter[0], (int) predictionPointCenter[1], (int) avoidTargetCenter[0], (int) avoidTargetCenter[1]);
//            }
        }
        if(predictionPointLeft != null)
        {
            gfx.setColor(Color.LIGHT_GRAY);
            gfx.drawOval((int) predictionPointLeft[0], (int) predictionPointLeft[1],1,1);
//            if(avoidTargetLeft != null){
//                gfx.setColor(Color.YELLOW);
//                gfx.drawLine((int) predictionPointLeft[0], (int) predictionPointLeft[1], (int) avoidTargetLeft[0], (int) avoidTargetLeft[1]);
//            }
        }
        if(predictionPointRight != null)
        {
            gfx.setColor(Color.CYAN);
            gfx.drawOval((int) predictionPointRight[0], (int) predictionPointRight[1],1,1);
//            if(avoidTargetRight != null){
//                gfx.setColor(Color.YELLOW);
//                gfx.drawLine((int) predictionPointRight[0], (int) predictionPointRight[1], (int) avoidTargetRight[0], (int) avoidTargetRight[1]);
//            }
        }
    }
}
