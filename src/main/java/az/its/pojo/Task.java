package az.its.pojo;

import java.io.Serializable;
import java.util.Date;

public class Task implements Serializable {
	private Integer id;
    private String from;
    private String to;
    private String name;
    private Date create;
    private Date start;
    private Date end;

    public Task() {
    }

    public Task(Integer id, String from, String to, String name, Date create, Date start, Date end) {
        this.id = id;
        this.from = from;
        this.to = to;
        this.name = name;
        this.create = create;
        this.start = start;
        this.end = end;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Date getCreate() {
        return create;
    }

    public void setCreate(Date create) {
        this.create = create;
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
