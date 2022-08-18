package com.xgy.reggie.dto;

import com.xgy.reggie.entity.Setmeal;
import com.xgy.reggie.entity.SetmealDish;
import lombok.Data;
import java.util.List;

@Data
public class SetmealDto extends Setmeal {

    private List<SetmealDish> setmealDishes;

    private String categoryName;
}
