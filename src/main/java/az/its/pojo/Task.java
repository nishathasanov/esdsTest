/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package az.its.pojo;

import java.io.Serializable;
import java.util.Date;

/**
 *
 * @author MuradI
 */
public class Task implements Serializable{
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private Integer id;
    private String actorId;
    private String name;
    private Date start;
    private Date end;

    public Task() {
    }

    public Task(Integer id, String actorId, String name, Date start, Date end) {
        this.id = id;
        this.actorId = actorId;
        this.name = name;
        this.start = start;
        this.end = end;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getActorId() {
        return actorId;
    }

    public void setActorId(String actorId) {
        this.actorId = actorId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Date getStart() {
        return start;
    }

    public void setStart(Date start) {
        this.start = start;
    }

    public Date getEnd() {
        return end;
    }

    public void setEnd(Date end) {
        this.end = end;
    }
}
